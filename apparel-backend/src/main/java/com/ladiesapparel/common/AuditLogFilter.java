package com.ladiesapparel.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Logs every state-changing request under /api/admin/** — who did what, when.
 * Runs AFTER JwtAuthFilter so SecurityContext is already populated.
 */
@Component
@RequiredArgsConstructor
public class AuditLogFilter extends OncePerRequestFilter {

    private final AuditLogRepository auditLogRepository;

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        boolean isAdminRoute = request.getRequestURI().startsWith("/api/admin/");
        boolean isMutating = MUTATING_METHODS.contains(request.getMethod());

        if (isAdminRoute && isMutating) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String adminEmail = auth != null ? auth.getName() : "unknown";

            try {
                AuditLog log = AuditLog.builder()
                        .adminEmail(adminEmail)
                        .httpMethod(request.getMethod())
                        .path(request.getRequestURI())
                        .statusCode(response.getStatus())
                        .build();
                auditLogRepository.save(log);
            } catch (Exception ignored) {
                // audit logging must never break the actual request
            }
        }
    }
}
