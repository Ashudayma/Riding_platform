package com.ridingplatform.driver.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverStatus;
import com.ridingplatform.driver.infrastructure.persistence.OnlineStatus;
import com.ridingplatform.driver.infrastructure.persistence.VehicleEntity;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.tracking.infrastructure.persistence.DriverLocationHistoryJpaRepository;
import com.ridingplatform.tracking.infrastructure.persistence.LocationProviderType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DriverAvailabilityServiceTest {

    private final DriverProfileResolver driverProfileResolver = Mockito.mock(DriverProfileResolver.class);
    private final DriverAvailabilityJpaRepository driverAvailabilityJpaRepository = Mockito.mock(DriverAvailabilityJpaRepository.class);
    private final DriverRedisStateService driverRedisStateService = Mockito.mock(DriverRedisStateService.class);
    private final DriverLocationEventPublisher driverLocationEventPublisher = Mockito.mock(DriverLocationEventPublisher.class);
    private final DriverLocationHistoryJpaRepository driverLocationHistoryJpaRepository = Mockito.mock(DriverLocationHistoryJpaRepository.class);
    private final RideJpaRepository rideJpaRepository = Mockito.mock(RideJpaRepository.class);
    private final IdGenerator idGenerator = Mockito.mock(IdGenerator.class);
    private final EntityManager entityManager = Mockito.mock(EntityManager.class);

    private DriverAvailabilityService driverAvailabilityService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-18T12:00:00Z"), ZoneOffset.UTC);
        driverAvailabilityService = new DriverAvailabilityService(
                driverProfileResolver,
                driverAvailabilityJpaRepository,
                driverRedisStateService,
                driverLocationEventPublisher,
                driverLocationHistoryJpaRepository,
                rideJpaRepository,
                idGenerator,
                clock,
                new ObjectMapper(),
                entityManager
        );
        when(idGenerator.nextId()).thenReturn(UUID.fromString("10000000-0000-0000-0000-000000000001"));
        when(driverAvailabilityJpaRepository.save(any(DriverAvailabilityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldMarkDriverOnlineAndStoreHotState() {
        DriverProfileEntity driverProfile = driverProfile();
        when(driverProfileResolver.currentDriverProfile()).thenReturn(driverProfile);
        when(driverAvailabilityJpaRepository.findByDriverProfileId(driverProfile.getId())).thenReturn(Optional.empty());

        DriverAvailabilityResult result = driverAvailabilityService.updateAvailability(new DriverAvailabilityCommand(
                true,
                (short) 4,
                "session-1",
                "1.0.0",
                "ANDROID"
        ));

        assertThat(result.onlineStatus()).isEqualTo(OnlineStatus.ONLINE);
        assertThat(result.availabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        verify(driverRedisStateService).updateState(any(), any(), anyDouble(), anyDouble(), anyBoolean());
    }

    @Test
    void shouldPublishLocationUpdateAndPersistFastState() {
        DriverProfileEntity driverProfile = driverProfile();
        DriverAvailabilityEntity availability = new DriverAvailabilityEntity();
        availability.setId(UUID.fromString("20000000-0000-0000-0000-000000000001"));
        availability.setDriverProfile(driverProfile);
        availability.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        availability.setOnlineStatus(OnlineStatus.ONLINE);
        when(driverProfileResolver.currentDriverProfile()).thenReturn(driverProfile);
        when(driverAvailabilityJpaRepository.findByDriverProfileId(driverProfile.getId())).thenReturn(Optional.of(availability));

        DriverLocationUpdateResult result = driverAvailabilityService.updateLiveLocation(new DriverLocationUpdateCommand(
                28.6139,
                77.2090,
                new BigDecimal("180"),
                new BigDecimal("32"),
                new BigDecimal("5"),
                LocationProviderType.GPS
        ));

        assertThat(result.latitude()).isEqualTo(28.6139);
        verify(driverLocationEventPublisher).publish(any());
    }

    @Test
    void shouldReturnNearbyDriversFromRedisHotState() {
        when(driverRedisStateService.searchNearby(any(double.class), any(double.class), any(double.class), any(int.class)))
                .thenReturn(List.of(Map.ofEntries(
                        Map.entry("driverProfileId", "40000000-0000-0000-0000-000000000001"),
                        Map.entry("vehicleId", "50000000-0000-0000-0000-000000000001"),
                        Map.entry("vehicleType", "SEDAN"),
                        Map.entry("availabilityStatus", "AVAILABLE"),
                        Map.entry("averageRating", "4.95"),
                        Map.entry("riskBlocked", "false"),
                        Map.entry("currentRideId", ""),
                        Map.entry("latitude", "28.6139"),
                        Map.entry("longitude", "77.2090"),
                        Map.entry("distanceMeters", "240.5"),
                        Map.entry("lastHeartbeatAt", "2026-04-18T12:00:00Z")
                )));

        List<NearbyDriverResult> results = driverAvailabilityService.searchNearby(new NearbyDriverSearchCommand(
                28.6139,
                77.2090,
                3000,
                10,
                AvailabilityStatus.AVAILABLE,
                VehicleType.SEDAN,
                true,
                new BigDecimal("4.50"),
                true
        ));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).vehicleType()).isEqualTo(VehicleType.SEDAN);
        assertThat(results.get(0).distanceMeters()).isEqualTo(240.5);
    }

    private DriverProfileEntity driverProfile() {
        DriverProfileEntity profile = new DriverProfileEntity();
        profile.setId(UUID.fromString("40000000-0000-0000-0000-000000000001"));
        profile.setAverageRating(new BigDecimal("4.95"));
        profile.setDriverStatus(DriverStatus.ACTIVE);
        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setId(UUID.fromString("50000000-0000-0000-0000-000000000001"));
        vehicle.setVehicleType(VehicleType.SEDAN);
        vehicle.setSeatCapacity((short) 4);
        profile.setCurrentVehicle(vehicle);
        return profile;
    }
}
