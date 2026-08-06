package com.ladiesapparel.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Rate-limits abuse-prone auth endpoints (login, OTP requests) per client IP.
 * Everything else passes through untouched.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;

    // path -> [max requests, window in seconds]
    private static final Map<String, int[]> LIMITED_PATHS = Map.of(
            "/api/auth/login", new int[]{10, 60},
            "/api/auth/register", new int[]{5, 60},
            "/api/auth/verify-otp", new int[]{10, 60},
            "/api/auth/resend-otp", new int[]{3, 60},
            "/api/auth/forgot-password", new int[]{3, 60}
    );

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        int[] limit = LIMITED_PATHS.get(request.getRequestURI());

        if (limit != null) {
            String clientIp = extractClientIp(request);
            String key = clientIp + ":" + request.getRequestURI();

            if (!rateLimiter.isAllowed(key, limit[0], limit[1])) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Too many requests. Please try again in a minute.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
