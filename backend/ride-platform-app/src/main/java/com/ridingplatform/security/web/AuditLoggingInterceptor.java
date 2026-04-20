package com.ridingplatform.security.web;

import com.ridingplatform.admin.infrastructure.persistence.AuditResultStatus;
import com.ridingplatform.config.SecurityProperties;
import com.ridingplatform.security.application.AdminAuditService;
import com.ridingplatform.security.application.SecurityContextFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuditLoggingInterceptor implements HandlerInterceptor {

    private final SecurityProperties securityProperties;
    private final AdminAuditService adminAuditService;
    private final SecurityContextFacade securityContextFacade;

    public AuditLoggingInterceptor(
            SecurityProperties securityProperties,
            AdminAuditService adminAuditService,
            SecurityContextFacade securityContextFacade
    ) {
        this.securityProperties = securityProperties;
        this.adminAuditService = adminAuditService;
        this.securityContextFacade = securityContextFacade;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!securityProperties.audit().enabled()) {
            return;
        }
        boolean auditedPath = securityProperties.audit().auditedPathPrefixes().stream()
                .anyMatch(prefix -> request.getRequestURI().startsWith(prefix));
        if (!auditedPath) {
            return;
        }
        boolean readOnly = "GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod());
        if (readOnly && !securityProperties.audit().includeReadOnlyAdminOperations()) {
            return;
        }
        AuditResultStatus resultStatus = response.getStatus() >= 200 && response.getStatus() < 400
                ? AuditResultStatus.SUCCESS
                : response.getStatus() == 403 ? AuditResultStatus.DENIED : AuditResultStatus.FAILED;
        adminAuditService.log(
                securityContextFacade.currentActor(),
                request.getMethod() + "_" + request.getRequestURI().replace('/', '_').replace('-', '_').toUpperCase(),
                resolveTargetType(request),
                resolveTargetId(request.getHeader("X-Target-Id")),
                resultStatus,
                response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER),
                response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER),
                clientIp(request),
                request.getHeader("User-Agent"),
                "{\"status\":" + response.getStatus() + "}"
        );
    }

    private String resolveTargetType(HttpServletRequest request) {
        String[] segments = request.getRequestURI().split("/");
        return segments.length >= 4 ? segments[3].toUpperCase() : "UNKNOWN";
    }

    private UUID resolveTargetId(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        return UUID.fromString(headerValue);
    }

    private String clientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(value -> value.split(",")[0].trim())
                .orElse(request.getRemoteAddr());
    }
}
