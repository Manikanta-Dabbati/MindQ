package com.mindq.security;

import com.mindq.common.metrics.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * Logs request method, URI, status code, and duration.
 * Records request success/failure in MetricsService.
 * Skips health check endpoints to avoid noise.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final MetricsService metricsService;

    private static final String[] SKIP_PATHS = {"/api/v1/health", "/api/v1/health/"};

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip health check endpoints
        if (isSkipPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();
        int status = 0;

        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();
        } catch (Exception e) {
            status = 500;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            boolean success = status < 400;

            metricsService.recordRequest(success);

            if (status >= 500) {
                log.error("{} {} {} {}ms", request.getMethod(), path, status, duration);
            } else if (status >= 400) {
                log.warn("{} {} {} {}ms", request.getMethod(), path, status, duration);
            } else {
                log.info("{} {} {} {}ms", request.getMethod(), path, status, duration);
            }
        }
    }

    private boolean isSkipPath(String path) {
        for (String skip : SKIP_PATHS) {
            if (path.equals(skip) || path.equals("/api/v1/health/live") || path.equals("/api/v1/health/ready")) {
                return true;
            }
        }
        return false;
    }
}
