package com.ridingplatform.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.security")
public record SecurityProperties(
        String issuerUri,
        String resourceClientId,
        String requiredAudience,
        int trustedProxyHops,
        RateLimit rateLimit,
        Idempotency idempotency,
        Audit audit
) {

    public record RateLimit(
            boolean enabled,
            long windowSeconds,
            long riderRequestsPerWindow,
            long driverRequestsPerWindow,
            long adminRequestsPerWindow,
            long supportRequestsPerWindow,
            long fraudRequestsPerWindow,
            long anonymousRequestsPerWindow
    ) {
    }

    public record Idempotency(
            boolean enabled,
            long ttlHours,
            List<String> protectedPaths
    ) {
    }

    public record Audit(
            boolean enabled,
            List<String> auditedPathPrefixes,
            boolean includeReadOnlyAdminOperations
    ) {
    }
}
