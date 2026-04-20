package com.ridingplatform.security.web;

import com.ridingplatform.config.SecurityProperties;
import com.ridingplatform.security.application.IdempotencyConflictException;
import com.ridingplatform.security.application.IdempotencyService;
import com.ridingplatform.security.application.SecurityContextFacade;
import com.ridingplatform.security.application.StoredIdempotentResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_METHODS = Set.of("POST", "PUT", "PATCH");

    private final SecurityProperties securityProperties;
    private final IdempotencyService idempotencyService;
    private final SecurityContextFacade securityContextFacade;
    private final ErrorResponseWriter errorResponseWriter;

    public IdempotencyFilter(
            SecurityProperties securityProperties,
            IdempotencyService idempotencyService,
            SecurityContextFacade securityContextFacade,
            ErrorResponseWriter errorResponseWriter
    ) {
        this.securityProperties = securityProperties;
        this.idempotencyService = idempotencyService;
        this.securityContextFacade = securityContextFacade;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!securityProperties.idempotency().enabled() || !PROTECTED_METHODS.contains(request.getMethod())) {
            return true;
        }
        return securityProperties.idempotency().protectedPaths().stream()
                .noneMatch(path -> request.getRequestURI().startsWith(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            errorResponseWriter.write(request, response, HttpStatus.BAD_REQUEST, "Idempotency-Key header is required for this operation");
            return;
        }
        String actorSubject = securityContextFacade.currentActor().map(actor -> actor.subject()).orElse("anonymous");
        CachedBodyHttpServletRequest requestWrapper = new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        String requestBody = new String(requestWrapper.getCachedBody(), StandardCharsets.UTF_8);
        String requestHash = sha256(request.getMethod() + ":" + request.getRequestURI() + ":" + requestBody);
        try {
            var existingResponse = idempotencyService.begin(actorSubject, idempotencyKey, request.getMethod(), request.getRequestURI(), requestHash);
            if (existingResponse.isPresent()) {
                replay(responseWrapper, existingResponse.get());
                return;
            }
            filterChain.doFilter(requestWrapper, responseWrapper);
            String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            idempotencyService.complete(actorSubject, idempotencyKey, responseWrapper.getStatus(), responseBody);
            responseWrapper.copyBodyToResponse();
        } catch (IdempotencyConflictException exception) {
            errorResponseWriter.write(request, response, HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    private void replay(ContentCachingResponseWrapper response, StoredIdempotentResponse stored) throws IOException {
        response.setStatus(stored.httpStatus());
        response.setContentType("application/json");
        if (stored.body() != null) {
            response.getWriter().write(stored.body());
        }
        response.copyBodyToResponse();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to compute request hash", exception);
        }
    }
}
