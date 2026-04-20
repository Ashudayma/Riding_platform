package com.ridingplatform.ride.application;

import com.ridingplatform.config.ApplicationProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class DriverAssignmentCandidateCache {

    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationProperties applicationProperties;

    public DriverAssignmentCandidateCache(
            StringRedisTemplate stringRedisTemplate,
            ApplicationProperties applicationProperties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.applicationProperties = applicationProperties;
    }

    public void put(UUID rideRequestId, List<DriverAssignmentCandidateScore> candidates) {
        if (candidates.isEmpty()) {
            stringRedisTemplate.delete(key(rideRequestId));
            return;
        }
        StringBuilder serialized = new StringBuilder();
        for (DriverAssignmentCandidateScore candidate : candidates) {
            if (serialized.length() > 0) {
                serialized.append('\n');
            }
            serialized.append(candidate.driverProfileId()).append('|')
                    .append(candidate.vehicleId()).append('|')
                    .append(candidate.totalScore()).append('|')
                    .append(candidate.distanceMeters()).append('|')
                    .append(candidate.etaSeconds());
        }
        stringRedisTemplate.opsForValue().set(
                key(rideRequestId),
                serialized.toString(),
                Duration.ofSeconds(applicationProperties.dispatch().candidateCacheSeconds())
        );
    }

    public List<UUID> getCachedDriverIds(UUID rideRequestId) {
        String payload = stringRedisTemplate.opsForValue().get(key(rideRequestId));
        if (payload == null || payload.isBlank()) {
            return List.of();
        }
        List<UUID> driverIds = new ArrayList<>();
        for (String row : payload.split("\n")) {
            String[] columns = row.split("\\|");
            if (columns.length >= 1 && !columns[0].isBlank()) {
                driverIds.add(UUID.fromString(columns[0]));
            }
        }
        return driverIds;
    }

    public void evict(UUID rideRequestId) {
        stringRedisTemplate.delete(key(rideRequestId));
    }

    private String key(UUID rideRequestId) {
        return "dispatch:candidates:" + rideRequestId;
    }
}
