package com.mindq.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Adds a unique request ID to every request/response.
 * The ID is returned as X-Request-Id header and stored in MDC for logging.
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }

        // Set on response
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Set in MDC for logging
        org.slf4j.MDC.put(REQUEST_ID_MDC_KEY, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            org.slf4j.MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
