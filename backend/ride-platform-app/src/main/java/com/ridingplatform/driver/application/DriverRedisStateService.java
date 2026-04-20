package com.ridingplatform.driver.application;

import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityEntity;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class DriverRedisStateService {

    private static final String DRIVER_GEO_KEY = "drivers:geo:all";
    private static final String DRIVER_STATE_PREFIX = "drivers:state:";

    private final StringRedisTemplate stringRedisTemplate;

    public DriverRedisStateService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void updateState(
            DriverAvailabilityEntity entity,
            VehicleType vehicleType,
            double latitude,
            double longitude,
            boolean riskBlocked
    ) {
        String driverId = entity.getDriverProfile().getId().toString();
        stringRedisTemplate.opsForGeo().add(DRIVER_GEO_KEY, new Point(longitude, latitude), driverId);
        Map<String, String> state = new HashMap<>();
        state.put("driverProfileId", driverId);
        state.put("vehicleId", entity.getDriverProfile().getCurrentVehicle() == null ? "" : entity.getDriverProfile().getCurrentVehicle().getId().toString());
        state.put("vehicleType", vehicleType == null ? "" : vehicleType.name());
        state.put("availabilityStatus", entity.getAvailabilityStatus().name());
        state.put("onlineStatus", entity.getOnlineStatus().name());
        state.put("currentRideId", entity.getCurrentRideId() == null ? "" : entity.getCurrentRideId().toString());
        state.put("averageRating", entity.getDriverProfile().getAverageRating().toPlainString());
        state.put("riskBlocked", Boolean.toString(riskBlocked));
        state.put("latitude", Double.toString(latitude));
        state.put("longitude", Double.toString(longitude));
        state.put("lastHeartbeatAt", entity.getLastHeartbeatAt() == null ? Instant.now().toString() : entity.getLastHeartbeatAt().toString());
        stringRedisTemplate.opsForHash().putAll(stateKey(driverId), state);
        stringRedisTemplate.expire(stateKey(driverId), Duration.ofMinutes(30));
    }

    public void markOffline(UUID driverProfileId) {
        String member = driverProfileId.toString();
        stringRedisTemplate.opsForGeo().remove(DRIVER_GEO_KEY, member);
        stringRedisTemplate.opsForHash().put(stateKey(member), "onlineStatus", "OFFLINE");
        stringRedisTemplate.opsForHash().put(stateKey(member), "availabilityStatus", AvailabilityStatus.OFFLINE.name());
    }

    public void updateRiskBlocked(UUID driverProfileId, boolean riskBlocked) {
        stringRedisTemplate.opsForHash().put(stateKey(driverProfileId.toString()), "riskBlocked", Boolean.toString(riskBlocked));
    }

    public List<Map<String, String>> searchNearby(double latitude, double longitude, double radiusMeters, int limit) {
        Circle within = new Circle(new Point(longitude, latitude), new Distance(radiusMeters / 1000.0d, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = stringRedisTemplate.opsForGeo()
                .radius(DRIVER_GEO_KEY, within, RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending().limit(limit));
        List<Map<String, String>> results = new ArrayList<>();
        if (geoResults == null) {
            return results;
        }
        for (GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult : geoResults) {
            String driverId = geoResult.getContent().getName();
            Map<Object, Object> state = stringRedisTemplate.opsForHash().entries(stateKey(driverId));
            if (!state.isEmpty()) {
                Map<String, String> mapped = new HashMap<>();
                state.forEach((key, value) -> mapped.put(String.valueOf(key), String.valueOf(value)));
                mapped.put("distanceMeters", geoResult.getDistance() == null ? "0" : Double.toString(geoResult.getDistance().getValue()));
                results.add(mapped);
            }
        }
        return results;
    }

    private String stateKey(String driverProfileId) {
        return DRIVER_STATE_PREFIX + driverProfileId;
    }
}
