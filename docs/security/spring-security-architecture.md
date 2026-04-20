# Spring Security Architecture

## Implemented Components

- JWT resource server with Spring Security 6 / Spring Boot 3
- Keycloak role extraction from both `realm_access` and `resource_access`
- Role-separated route authorization
- Correlation ID filter
- Secure request logging filter
- Redis-backed rate limiting filter
- Idempotency filter for booking/payment-like operations
- Audit logging interceptor for privileged and sensitive endpoints
- Centralized exception handling with safe messages

## Main Code Entry Points

- Security config:
  [SecurityConfiguration.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/config/SecurityConfiguration.java)
- Security properties:
  [SecurityProperties.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/config/SecurityProperties.java)
- Rate limiting:
  [RateLimitService.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/security/application/RateLimitService.java)
  [RateLimitingFilter.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/security/web/RateLimitingFilter.java)
- Idempotency:
  [IdempotencyService.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/security/application/IdempotencyService.java)
  [IdempotencyFilter.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/security/web/IdempotencyFilter.java)
- Audit:
  [AdminAuditService.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/security/application/AdminAuditService.java)
  [AuditLoggingInterceptor.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/security/web/AuditLoggingInterceptor.java)

## Validation and Exception Handling

- Validation errors return sanitized 400 responses
- Authorization failures return 403 without internal details
- Rate-limited requests return 429
- Unhandled failures are logged server-side and return generic 500 responses

## Production Notes

- Keep issuer and audience validation aligned with gateway and Keycloak configuration
- Add gateway-level rate limiting and WAF rules in front of the application
- Move high-value admin/fraud operations behind MFA-enforced clients
- Extend idempotency protection to payouts, refunds, and coupon issuance as those APIs are added
