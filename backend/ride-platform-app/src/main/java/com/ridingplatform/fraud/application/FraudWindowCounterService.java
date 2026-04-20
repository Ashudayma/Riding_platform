package com.ridingplatform.fraud.application;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class FraudWindowCounterService {

    private final StringRedisTemplate stringRedisTemplate;

    public FraudWindowCounterService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long increment(String key, Duration ttl) {
        Long current = stringRedisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            stringRedisTemplate.expire(key, ttl);
        }
        return current == null ? 0 : current;
    }
}
