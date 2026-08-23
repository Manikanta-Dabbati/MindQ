package com.mindq.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory sliding-window rate limiter.
 * Tracks request timestamps per IP+category. Returns 429 when limit exceeded.
 *
 * Categories and defaults:
 *   AUTH:     5 req/min  (login, register, forgot-password, reset-password)
 *   AI:       10 req/min (mcq/generate)
 *   UPLOAD:   10 req/min (materials/upload)
 *   GENERAL:  60 req/min (everything else)
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    public RateLimitFilter(Environment environment) {
        this.environment = environment;
    }

    private static final long WINDOW_MS = 60_000; // 1 minute

    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private final Environment environment; // injected via constructor above

    @Value("${app.rate-limit.auth-per-minute:5}")
    private int authLimit;

    @Value("${app.rate-limit.ai-per-minute:10}")
    private int aiLimit;

    @Value("${app.rate-limit.upload-per-minute:10}")
    private int uploadLimit;

    @Value("${app.rate-limit.general-per-minute:60}")
    private int generalLimit;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);
        String path = request.getRequestURI();
        String category = categorize(path, request.getMethod());

        int limit = switch (category) {
            case "AUTH" -> authLimit;
            case "AI" -> aiLimit;
            case "UPLOAD" -> uploadLimit;
            default -> generalLimit;
        };

        String key = ip + ":" + category;
        RateLimiter limiter = limiters.computeIfAbsent(key, k -> new RateLimiter());

        if (!limiter.tryAcquire(limit, WINDOW_MS)) {
            log.warn("Rate limit exceeded: ip={}, category={}, limit={}/min", ip, category, limit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"success":false,"message":"Too many requests. Please try again later.","data":null}
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Skip rate limiting for non-API paths (static resources, error page, etc.).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Skip rate limiting for non-API paths and during tests
        if (!path.startsWith("/api/")) {
            return true;
        }
        return Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

    private String categorize(String path, String method) {
        if (path.contains("/auth/login") || path.contains("/auth/register")
                || path.contains("/auth/forgot-password") || path.contains("/auth/reset-password")) {
            return "AUTH";
        }
        if (path.contains("/mcq/generate")) {
            return "AI";
        }
        if (path.contains("/materials/upload")) {
            return "UPLOAD";
        }
        return "GENERAL";
    }

    private String getClientIp(HttpServletRequest request) {
        // When behind a trusted proxy (nginx/cloudflare), use the rightmost untrusted IP
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // In production behind a single proxy, the first IP is the client
            // Behind multiple proxies, use the rightmost IP that's not a known proxy
            String[] ips = xForwardedFor.split(",");
            // For simplicity, use the first IP (client IP) when behind a single proxy
            // For multi-proxy setups, configure server.forward-headers-strategy=native
            return ips[0].trim();
        }
        String forwarded = request.getHeader("X-Real-IP");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Periodic cleanup of stale rate limiter entries.
     * Runs every 5 minutes to prevent unbounded memory growth.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 300_000)
    public void cleanupStaleEntries() {
        long now = System.currentTimeMillis();
        long cutoff = now - WINDOW_MS * 2; // Keep entries from last 2 minutes
        
        Iterator<Map.Entry<String, RateLimiter>> it = limiters.entrySet().iterator();
        int removed = 0;
        while (it.hasNext()) {
            Map.Entry<String, RateLimiter> entry = it.next();
            RateLimiter limiter = entry.getValue();
            // Remove if deque is empty or all timestamps are old
            if (limiter.isStale(cutoff)) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Rate limiter cleanup: removed {} stale entries, {} remaining", removed, limiters.size());
        }
    }

    /**
     * Sliding-window rate limiter using a deque of timestamps.
     */
    private static class RateLimiter {
        private final Deque<Long> timestamps = new ConcurrentLinkedDeque<>();

        boolean tryAcquire(int limit, long windowMs) {
            long now = System.currentTimeMillis();
            long cutoff = now - windowMs;

            // Evict old entries
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= limit) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }

        /**
         * Check if this limiter has no recent activity.
         */
        boolean isStale(long cutoff) {
            // Evict old entries first
            while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
                timestamps.pollFirst();
            }
            return timestamps.isEmpty();
        }
    }
}
