package com.ridingplatform.pricing.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisBackedSurgePricingService implements SurgePricingService {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisBackedSurgePricingService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public BigDecimal surgeMultiplier(PricingRequestContext context) {
        String zoneKey = context.zoneCode() == null ? "default" : context.zoneCode();
        String value = stringRedisTemplate.opsForValue().get("pricing:surge:" + context.cityCode() + ":" + zoneKey);
        if (value == null || value.isBlank()) {
            return BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP);
        }
        return new BigDecimal(value).setScale(4, RoundingMode.HALF_UP);
    }
}
