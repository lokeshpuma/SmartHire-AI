package com.example.resumescreening.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Token-bucket in-memory rate limiter for login and resume upload endpoints.
 * Conforms to PRD Section 4.1.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final int loginCapacity;
    private final int uploadCapacity;

    // key -> (bucket window timestamp, count)
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${app.security.rate-limit.login-capacity:10}") int loginCapacity,
            @Value("${app.security.rate-limit.upload-capacity:30}") int uploadCapacity) {
        this.loginCapacity = loginCapacity;
        this.uploadCapacity = uploadCapacity;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isRateLimitedEndpoint(path, method)) {
            String clientIp = getClientIp(request);
            String bucketKey = path.contains("auth/login") ? "login:" + clientIp : "upload:" + clientIp;
            int limit = path.contains("auth/login") ? loginCapacity : uploadCapacity;

            if (!allowRequest(bucketKey, limit)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Please wait a minute before retrying.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedEndpoint(String path, String method) {
        if ("POST".equalsIgnoreCase(method)) {
            if (path.endsWith("/api/v1/auth/login")) {
                return true;
            }
            if (path.contains("/api/v1/jobs/") && path.contains("/resumes")) {
                return true;
            }
        }
        return false;
    }

    private boolean allowRequest(String key, int maxRequestsPerMinute) {
        long currentMinute = System.currentTimeMillis() / 60000;
        TokenBucket bucket = buckets.compute(key, (k, existing) -> {
            if (existing == null || existing.minuteWindow != currentMinute) {
                return new TokenBucket(currentMinute, new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        // Periodic cleanup to avoid memory leak
        if (buckets.size() > 5000) {
            buckets.entrySet().removeIf(e -> e.getValue().minuteWindow < currentMinute - 2);
        }

        return bucket.counter.get() <= maxRequestsPerMinute;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    private static class TokenBucket {
        final long minuteWindow;
        final AtomicInteger counter;

        TokenBucket(long minuteWindow, AtomicInteger counter) {
            this.minuteWindow = minuteWindow;
            this.counter = counter;
        }
    }
}
