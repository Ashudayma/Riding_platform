package com.ridingplatform.realtime.application;

import com.ridingplatform.config.ApplicationProperties;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DriverLocationFanoutGuard {

    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationProperties applicationProperties;

    public DriverLocationFanoutGuard(
            StringRedisTemplate stringRedisTemplate,
            ApplicationProperties applicationProperties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.applicationProperties = applicationProperties;
    }

    public boolean shouldPublish(UUID rideId, double latitude, double longitude) {
        if (rideId == null) {
            return false;
        }
        String key = "realtime:location:" + rideId;
        String current = format(latitude, longitude);
        String previous = stringRedisTemplate.opsForValue().get(key);
        if (previous != null) {
            String[] parts = previous.split("\\|");
            if (parts.length == 2) {
                double previousLat = Double.parseDouble(parts[0]);
                double previousLon = Double.parseDouble(parts[1]);
                double movement = metersBetween(previousLat, previousLon, latitude, longitude);
                if (movement < applicationProperties.realtime().minimumLocationDistanceMeters()) {
                    return false;
                }
            }
        }
        Boolean accepted = stringRedisTemplate.opsForValue().setIfAbsent(
                key + ":lock",
                "1",
                Duration.ofMillis(applicationProperties.realtime().outboundLocationThrottleMillis())
        );
        if (!Boolean.TRUE.equals(accepted)) {
            return false;
        }
        stringRedisTemplate.opsForValue().set(key, current, Duration.ofMinutes(5));
        return true;
    }

    private String format(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.6f|%.6f", latitude, longitude);
    }

    private double metersBetween(double lat1, double lon1, double lat2, double lon2) {
        double latDiff = lat2 - lat1;
        double lonDiff = lon2 - lon1;
        return Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111_000d;
    }
}
