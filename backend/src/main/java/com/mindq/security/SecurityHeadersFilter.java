package com.mindq.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds security headers to every response.
 * Covers OWASP recommended headers for API backends.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Prevent MIME type sniffing
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // XSS protection (legacy browsers)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer policy — send origin only on cross-origin requests
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions policy — disable unnecessary browser features
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // Content Security Policy — strict policy for API backend
        // For API-only backend: no scripts, no styles, no images needed
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'; form-action 'none'");

        // HSTS — only set when running behind HTTPS (e.g. production with reverse proxy)
        String scheme = request.getScheme();
        if ("https".equals(scheme)) {
            response.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains");
        }

        // Cache control for API responses
        String path = request.getRequestURI();
        if (path.startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            response.setHeader("Pragma", "no-cache");
        }

        filterChain.doFilter(request, response);
    }
}
