package com.ridingplatform.security.application;

import com.ridingplatform.config.SecurityProperties;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;
    private final SecurityProperties securityProperties;

    public RateLimitService(StringRedisTemplate stringRedisTemplate, SecurityProperties securityProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.securityProperties = securityProperties;
    }

    public Optional<Long> validateAndCount(String subjectKey, String roleBucket, String routeKey) {
        if (!securityProperties.rateLimit().enabled()) {
            return Optional.empty();
        }
        long limit = switch (roleBucket) {
            case "ROLE_PLATFORM_ADMIN", "ROLE_OPS_ADMIN" -> securityProperties.rateLimit().adminRequestsPerWindow();
            case "ROLE_SUPPORT_AGENT" -> securityProperties.rateLimit().supportRequestsPerWindow();
            case "ROLE_FRAUD_ANALYST" -> securityProperties.rateLimit().fraudRequestsPerWindow();
            case "ROLE_DRIVER" -> securityProperties.rateLimit().driverRequestsPerWindow();
            case "ROLE_RIDER" -> securityProperties.rateLimit().riderRequestsPerWindow();
            default -> securityProperties.rateLimit().anonymousRequestsPerWindow();
        };
        String key = "rate-limit:%s:%s:%s".formatted(roleBucket, subjectKey, routeKey);
        Long current = stringRedisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(securityProperties.rateLimit().windowSeconds()));
        }
        if (current != null && current > limit) {
            throw new RateLimitExceededException("Request rate limit exceeded");
        }
        return Optional.ofNullable(current);
    }
}
