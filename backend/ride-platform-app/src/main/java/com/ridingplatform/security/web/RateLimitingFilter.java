package com.ridingplatform.security.web;

import com.ridingplatform.config.SecurityProperties;
import com.ridingplatform.security.application.RateLimitExceededException;
import com.ridingplatform.security.application.RateLimitService;
import com.ridingplatform.security.application.SecurityContextFacade;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> EXCLUDED_PREFIXES = Set.of("/actuator", "/swagger-ui", "/v3/api-docs");

    private final RateLimitService rateLimitService;
    private final SecurityContextFacade securityContextFacade;
    private final SecurityProperties securityProperties;
    private final ErrorResponseWriter errorResponseWriter;

    public RateLimitingFilter(
            RateLimitService rateLimitService,
            SecurityContextFacade securityContextFacade,
            SecurityProperties securityProperties,
            ErrorResponseWriter errorResponseWriter
    ) {
        this.rateLimitService = rateLimitService;
        this.securityContextFacade = securityContextFacade;
        this.securityProperties = securityProperties;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return EXCLUDED_PREFIXES.stream().anyMatch(prefix -> request.getRequestURI().startsWith(prefix))
                || !securityProperties.rateLimit().enabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String subject = securityContextFacade.currentActor().map(actor -> actor.subject()).orElse("anonymous:" + request.getRemoteAddr());
            String roleBucket = SecurityContextHolder.getContext().getAuthentication() == null
                    ? "ANONYMOUS"
                    : SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .findFirst()
                    .orElse("ANONYMOUS");
            rateLimitService.validateAndCount(subject, roleBucket, request.getRequestURI());
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException exception) {
            errorResponseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, exception.getMessage());
        }
    }
}
