package com.ridingplatform.pricing.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.pricing.infrastructure.persistence.PricingRuleSetEntity;
import com.ridingplatform.pricing.infrastructure.persistence.PricingRuleSetJpaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class PricingRuleCacheService {

    private final PricingRuleSetJpaRepository pricingRuleSetJpaRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PricingRuleCacheService(
            PricingRuleSetJpaRepository pricingRuleSetJpaRepository,
            StringRedisTemplate stringRedisTemplate,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.pricingRuleSetJpaRepository = pricingRuleSetJpaRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public PricingRuleSnapshot resolve(PricingRequestContext context) {
        String cacheKey = cacheKey(context);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            try {
                return objectMapper.readValue(cached, PricingRuleSnapshot.class);
            } catch (JsonProcessingException ignored) {
                stringRedisTemplate.delete(cacheKey);
            }
        }
        PricingRuleSetEntity entity = pricingRuleSetJpaRepository.findApplicableRules(
                        context.cityCode(),
                        context.zoneCode(),
                        context.rideType(),
                        context.vehicleType(),
                        Instant.now(clock)
                ).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active pricing rule configured for " + context));
        PricingRuleSnapshot snapshot = toSnapshot(entity);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(snapshot), Duration.ofMinutes(5));
        } catch (JsonProcessingException ignored) {
            // fall through without cache
        }
        return snapshot;
    }

    private PricingRuleSnapshot toSnapshot(PricingRuleSetEntity entity) {
        return new PricingRuleSnapshot(
                entity.getId(),
                entity.getCityCode(),
                entity.getZoneCode(),
                entity.getPricingVersion(),
                entity.getCurrencyCode(),
                entity.getBaseFare(),
                entity.getMinimumFare(),
                entity.getBookingFee(),
                entity.getPerKmRate(),
                entity.getPerMinuteRate(),
                entity.getPerStopCharge(),
                entity.getWaitingChargePerMinute(),
                entity.getCancellationBaseCharge(),
                entity.getCancellationPerKmCharge(),
                entity.getSharedDiscountFactor(),
                entity.getTaxPercentage(),
                entity.getSurgeCapMultiplier(),
                entity.getNightSurchargePercentage(),
                entity.getAirportSurchargeAmount()
        );
    }

    private String cacheKey(PricingRequestContext context) {
        return "pricing:rule:%s:%s:%s:%s".formatted(
                context.cityCode(),
                context.zoneCode() == null ? "default" : context.zoneCode(),
                context.rideType(),
                context.vehicleType() == null ? "default" : context.vehicleType()
        );
    }
}
