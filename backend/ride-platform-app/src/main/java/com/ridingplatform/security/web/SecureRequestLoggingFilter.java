package com.ridingplatform.security.web;

import com.ridingplatform.security.application.SecurityContextFacade;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SecureRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SecureRequestLoggingFilter.class);
    private final SecurityContextFacade securityContextFacade;

    public SecureRequestLoggingFilter(SecurityContextFacade securityContextFacade) {
        this.securityContextFacade = securityContextFacade;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String subject = securityContextFacade.currentActor().map(actor -> actor.subject()).orElse("anonymous");
            log.info(
                    "http_request method={} path={} status={} durationMs={} subject={} clientIp={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs,
                    subject,
                    resolveClientIp(request)
            );
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
