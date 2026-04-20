package com.ridingplatform.driver.application;

import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.common.persistence.GeoFactory;
import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.OnlineStatus;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.tracking.infrastructure.persistence.DriverLocationHistoryEntity;
import com.ridingplatform.tracking.infrastructure.persistence.DriverLocationHistoryJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverAvailabilityService {

    private final DriverProfileResolver driverProfileResolver;
    private final DriverAvailabilityJpaRepository driverAvailabilityJpaRepository;
    private final DriverRedisStateService driverRedisStateService;
    private final DriverLocationEventPublisher driverLocationEventPublisher;
    private final DriverLocationHistoryJpaRepository driverLocationHistoryJpaRepository;
    private final RideJpaRepository rideJpaRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public DriverAvailabilityService(
            DriverProfileResolver driverProfileResolver,
            DriverAvailabilityJpaRepository driverAvailabilityJpaRepository,
            DriverRedisStateService driverRedisStateService,
            DriverLocationEventPublisher driverLocationEventPublisher,
            DriverLocationHistoryJpaRepository driverLocationHistoryJpaRepository,
            RideJpaRepository rideJpaRepository,
            IdGenerator idGenerator,
            Clock clock,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            EntityManager entityManager
    ) {
        this.driverProfileResolver = driverProfileResolver;
        this.driverAvailabilityJpaRepository = driverAvailabilityJpaRepository;
        this.driverRedisStateService = driverRedisStateService;
        this.driverLocationEventPublisher = driverLocationEventPublisher;
        this.driverLocationHistoryJpaRepository = driverLocationHistoryJpaRepository;
        this.rideJpaRepository = rideJpaRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public DriverAvailabilityResult updateAvailability(DriverAvailabilityCommand command) {
        DriverProfileEntity driverProfile = driverProfileResolver.currentDriverProfile();
        DriverAvailabilityEntity entity = driverAvailabilityJpaRepository.findByDriverProfileId(driverProfile.getId())
                .orElseGet(() -> createAvailability(driverProfile));
        entity.setCurrentSessionId(command.sessionId());
        entity.setAvailableSeatCount(command.availableSeatCount());
        entity.setAppVersion(command.appVersion());
        entity.setDevicePlatform(command.devicePlatform());
        entity.setLastHeartbeatAt(Instant.now(clock));
        entity.setOnlineStatus(command.online() ? OnlineStatus.ONLINE : OnlineStatus.OFFLINE);
        entity.setAvailabilityStatus(command.online() ? AvailabilityStatus.AVAILABLE : AvailabilityStatus.OFFLINE);
        entity.setAvailableSince(command.online() ? Instant.now(clock) : null);
        driverAvailabilityJpaRepository.save(entity);
        if (command.online()) {
            double latitude = entity.getLastLocation() == null ? 0 : entity.getLastLocation().getY();
            double longitude = entity.getLastLocation() == null ? 0 : entity.getLastLocation().getX();
            driverRedisStateService.updateState(entity, currentVehicleType(driverProfile), latitude, longitude, isRiskBlocked(driverProfile));
        } else {
            driverRedisStateService.markOffline(driverProfile.getId());
        }
        return toAvailabilityResult(entity);
    }

    @Transactional
    public DriverLocationUpdateResult updateLiveLocation(DriverLocationUpdateCommand command) {
        DriverProfileEntity driverProfile = driverProfileResolver.currentDriverProfile();
        DriverAvailabilityEntity entity = driverAvailabilityJpaRepository.findByDriverProfileId(driverProfile.getId())
                .orElseGet(() -> createAvailability(driverProfile));
        Instant now = Instant.now(clock);
        entity.setLastLocation(GeoFactory.point(command.latitude(), command.longitude()));
        entity.setLastLocationAccuracyMeters(command.accuracyMeters());
        entity.setLastHeartbeatAt(now);
        if (entity.getOnlineStatus() != OnlineStatus.ONLINE) {
            entity.setOnlineStatus(OnlineStatus.ONLINE);
        }
        if (entity.getAvailabilityStatus() == AvailabilityStatus.OFFLINE) {
            entity.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            entity.setAvailableSince(now);
        }
        if (entity.getAvailabilityStatus() == AvailabilityStatus.AVAILABLE && entity.getAvailableSince() == null) {
            entity.setAvailableSince(now);
        }
        driverAvailabilityJpaRepository.save(entity);
        driverRedisStateService.updateState(entity, currentVehicleType(driverProfile), command.latitude(), command.longitude(), isRiskBlocked(driverProfile));
        driverLocationEventPublisher.publish(new DriverLocationEvent(
                idGenerator.nextId(),
                driverProfile.getId(),
                entity.getCurrentRideId(),
                currentVehicleType(driverProfile),
                command.latitude(),
                command.longitude(),
                command.headingDegrees(),
                command.speedKph(),
                command.accuracyMeters(),
                command.locationProvider(),
                now
        ));
        return new DriverLocationUpdateResult(driverProfile.getId(), command.latitude(), command.longitude(), now);
    }

    @Transactional(readOnly = true)
    public List<NearbyDriverResult> searchNearby(NearbyDriverSearchCommand command) {
        List<NearbyDriverResult> redisResults = driverRedisStateService.searchNearby(
                        command.latitude(),
                        command.longitude(),
                        command.radiusMeters(),
                        Math.max(command.limit() * 3, command.limit())
                ).stream()
                .filter(state -> command.availabilityStatus() == null || command.availabilityStatus().name().equals(state.get("availabilityStatus")))
                .filter(state -> command.vehicleType() == null || command.vehicleType().name().equals(state.get("vehicleType")))
                .filter(state -> !command.excludeActiveRide() || state.get("currentRideId") == null || state.get("currentRideId").isBlank())
                .filter(state -> command.minimumRating() == null || new java.math.BigDecimal(state.getOrDefault("averageRating", "0")).compareTo(command.minimumRating()) >= 0)
                .filter(state -> !command.excludeRiskBlocked() || !"true".equalsIgnoreCase(state.getOrDefault("riskBlocked", "false")))
                .map(this::mapRedisResult)
                .sorted(Comparator.comparingDouble(NearbyDriverResult::distanceMeters))
                .limit(command.limit())
                .toList();
        if (!redisResults.isEmpty()) {
            return redisResults;
        }
        return driverAvailabilityJpaRepository.searchNearbyPostgis(
                        command.latitude(),
                        command.longitude(),
                        command.radiusMeters(),
                        command.limit(),
                        command.availabilityStatus() == null ? null : command.availabilityStatus().name(),
                        command.vehicleType() == null ? null : command.vehicleType().name(),
                        command.excludeActiveRide(),
                        command.minimumRating(),
                        command.excludeRiskBlocked()
                ).stream()
                .map(this::mapEntityResult)
                .toList();
    }

    @KafkaListener(topics = "riding-platform.tracking.location-updated", groupId = "driver-location-history")
    @Transactional
    public void consumeLocationUpdate(String payload) throws Exception {
        DriverLocationEvent event = objectMapper.readValue(payload, DriverLocationEvent.class);
        DriverLocationHistoryEntity entity = new DriverLocationHistoryEntity();
        entity.setId(event.eventId());
        entity.setDriverProfile(entityManager.getReference(DriverProfileEntity.class, event.driverProfileId()));
        entity.setRide(resolveRide(event.currentRideId()).orElse(null));
        entity.setLocation(GeoFactory.point(event.latitude(), event.longitude()));
        entity.setHeadingDegrees(event.headingDegrees());
        entity.setSpeedKph(event.speedKph());
        entity.setAccuracyMeters(event.accuracyMeters());
        entity.setLocationProvider(event.locationProvider());
        entity.setCapturedAt(event.observedAt());
        entity.setReceivedAt(Instant.now(clock));
        entity.setCreatedAt(Instant.now(clock));
        driverLocationHistoryJpaRepository.save(entity);
    }

    private DriverAvailabilityEntity createAvailability(DriverProfileEntity driverProfile) {
        DriverAvailabilityEntity entity = new DriverAvailabilityEntity();
        entity.setId(idGenerator.nextId());
        entity.setDriverProfile(driverProfile);
        entity.setAvailableSeatCount((short) (driverProfile.getCurrentVehicle() == null ? 0 : driverProfile.getCurrentVehicle().getSeatCapacity()));
        entity.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
        entity.setOnlineStatus(OnlineStatus.OFFLINE);
        return entity;
    }

    private DriverAvailabilityResult toAvailabilityResult(DriverAvailabilityEntity entity) {
        double latitude = entity.getLastLocation() == null ? 0 : entity.getLastLocation().getY();
        double longitude = entity.getLastLocation() == null ? 0 : entity.getLastLocation().getX();
        return new DriverAvailabilityResult(
                entity.getDriverProfile().getId(),
                entity.getAvailabilityStatus(),
                entity.getOnlineStatus(),
                entity.getAvailableSeatCount(),
                entity.getCurrentRideId(),
                latitude,
                longitude,
                entity.getLastLocationAccuracyMeters(),
                entity.getLastHeartbeatAt()
        );
    }

    private NearbyDriverResult mapRedisResult(Map<String, String> state) {
        return new NearbyDriverResult(
                UUID.fromString(state.get("driverProfileId")),
                state.get("vehicleId") == null || state.get("vehicleId").isBlank() ? null : UUID.fromString(state.get("vehicleId")),
                state.get("vehicleType") == null || state.get("vehicleType").isBlank() ? null : VehicleType.valueOf(state.get("vehicleType")),
                AvailabilityStatus.valueOf(state.get("availabilityStatus")),
                new java.math.BigDecimal(state.getOrDefault("averageRating", "0")),
                Boolean.parseBoolean(state.getOrDefault("riskBlocked", "false")),
                state.get("currentRideId") == null || state.get("currentRideId").isBlank() ? null : UUID.fromString(state.get("currentRideId")),
                Double.parseDouble(state.getOrDefault("latitude", "0")),
                Double.parseDouble(state.getOrDefault("longitude", "0")),
                Double.parseDouble(state.getOrDefault("distanceMeters", "0")),
                Instant.parse(state.getOrDefault("lastHeartbeatAt", Instant.EPOCH.toString()))
        );
    }

    private NearbyDriverResult mapEntityResult(DriverAvailabilityEntity entity) {
        double latitude = entity.getLastLocation() == null ? 0 : entity.getLastLocation().getY();
        double longitude = entity.getLastLocation() == null ? 0 : entity.getLastLocation().getX();
        return new NearbyDriverResult(
                entity.getDriverProfile().getId(),
                entity.getDriverProfile().getCurrentVehicle() == null ? null : entity.getDriverProfile().getCurrentVehicle().getId(),
                currentVehicleType(entity.getDriverProfile()),
                entity.getAvailabilityStatus(),
                entity.getDriverProfile().getAverageRating(),
                isRiskBlocked(entity.getDriverProfile()),
                entity.getCurrentRideId(),
                latitude,
                longitude,
                0,
                entity.getLastHeartbeatAt()
        );
    }

    private VehicleType currentVehicleType(DriverProfileEntity driverProfile) {
        return driverProfile.getCurrentVehicle() == null ? null : driverProfile.getCurrentVehicle().getVehicleType();
    }

    private Optional<RideEntity> resolveRide(UUID currentRideId) {
        if (currentRideId == null) {
            return Optional.empty();
        }
        return rideJpaRepository.findById(currentRideId);
    }

    private boolean isRiskBlocked(DriverProfileEntity driverProfile) {
        return driverProfile.isFraudBlocked() || "BLOCKED".equals(driverProfile.getDriverStatus().name());
    }
}
