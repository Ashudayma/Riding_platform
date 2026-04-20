package com.ridingplatform.ride.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.driver.application.DriverAvailabilityService;
import com.ridingplatform.driver.application.DriverDispatchCandidate;
import com.ridingplatform.driver.application.NearbyDriverResult;
import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverStatus;
import com.ridingplatform.driver.infrastructure.persistence.OnlineStatus;
import com.ridingplatform.driver.infrastructure.persistence.VehicleEntity;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptEntity;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptStatus;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideLifecycleStatus;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import com.ridingplatform.ride.infrastructure.persistence.RideStatusHistoryJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

class DriverAssignmentServiceTest {

    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository = Mockito.mock(SpringDataRideRequestJpaRepository.class);
    private final RideJpaRepository rideJpaRepository = Mockito.mock(RideJpaRepository.class);
    private final DriverAssignmentAttemptJpaRepository driverAssignmentAttemptJpaRepository = Mockito.mock(DriverAssignmentAttemptJpaRepository.class);
    private final DriverAvailabilityJpaRepository driverAvailabilityJpaRepository = Mockito.mock(DriverAvailabilityJpaRepository.class);
    private final DriverProfileJpaRepository driverProfileJpaRepository = Mockito.mock(DriverProfileJpaRepository.class);
    private final RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository = Mockito.mock(RideStatusHistoryJpaRepository.class);
    private final DriverAvailabilityService driverAvailabilityService = Mockito.mock(DriverAvailabilityService.class);
    private final DriverAssignmentScoreCalculator driverAssignmentScoreCalculator = Mockito.mock(DriverAssignmentScoreCalculator.class);
    private final DriverAssignmentCandidateCache driverAssignmentCandidateCache = Mockito.mock(DriverAssignmentCandidateCache.class);
    private final RideEventPublisher rideEventPublisher = Mockito.mock(RideEventPublisher.class);
    private final IdGenerator idGenerator = Mockito.mock(IdGenerator.class);

    private DriverAssignmentService driverAssignmentService;
    private DriverProfileEntity driverProfile;
    private VehicleEntity vehicle;
    private RideRequestEntity rideRequest;
    private RideEntity ride;

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties(
                "riding-platform",
                new ApplicationProperties.Dispatch(
                        15,
                        10,
                        20,
                        100,
                        2,
                        3,
                        22,
                        new ApplicationProperties.Score(0.24, 0.22, 0.12, 0.12, 0.10, 0.08, 0.08, 0.02, 0.02, 1800, 1800, 5000)
                ),
                new ApplicationProperties.SharedRide(12, 3, 3, 10, 30, 4000, 0.55d, 0.18d),
                new ApplicationProperties.Realtime(10000, 1000, 20, 90),
                new ApplicationProperties.Fraud(4, 3, 5, 4, 24, 6, 24, 12, 24, 120, 140, 35, 1500, 420, 25, 50, 80, 12)
        );
        Clock clock = Clock.fixed(Instant.parse("2026-04-18T12:00:00Z"), ZoneOffset.UTC);
        driverAssignmentService = new DriverAssignmentService(
                properties,
                rideRequestJpaRepository,
                rideJpaRepository,
                driverAssignmentAttemptJpaRepository,
                driverAvailabilityJpaRepository,
                driverProfileJpaRepository,
                rideStatusHistoryJpaRepository,
                driverAvailabilityService,
                driverAssignmentScoreCalculator,
                driverAssignmentCandidateCache,
                rideEventPublisher,
                idGenerator,
                clock,
                new ObjectMapper()
        );

        when(rideRequestJpaRepository.save(any(RideRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideJpaRepository.save(any(RideEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(driverAssignmentAttemptJpaRepository.save(any(DriverAssignmentAttemptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(driverAvailabilityJpaRepository.save(any(DriverAvailabilityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idGenerator.nextId()).thenReturn(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                UUID.fromString("10000000-0000-0000-0000-000000000003"),
                UUID.fromString("10000000-0000-0000-0000-000000000004"),
                UUID.fromString("10000000-0000-0000-0000-000000000005"),
                UUID.fromString("10000000-0000-0000-0000-000000000006"),
                UUID.fromString("10000000-0000-0000-0000-000000000007"),
                UUID.fromString("10000000-0000-0000-0000-000000000008")
        );

        driverProfile = new DriverProfileEntity();
        driverProfile.setId(UUID.fromString("40000000-0000-0000-0000-000000000001"));
        driverProfile.setDriverStatus(DriverStatus.ACTIVE);
        vehicle = new VehicleEntity();
        vehicle.setId(UUID.fromString("50000000-0000-0000-0000-000000000001"));
        vehicle.setVehicleType(VehicleType.SEDAN);
        vehicle.setSeatCapacity((short) 4);
        driverProfile.setCurrentVehicle(vehicle);

        rideRequest = new RideRequestEntity();
        rideRequest.setId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
        rideRequest.setRequestStatus(RideRequestStatusEntityType.SEARCHING_DRIVER);
        rideRequest.setRequestedVehicleType(VehicleType.SEDAN);
        rideRequest.setSeatCount((short) 1);
        rideRequest.setOrigin(point(77.2090, 28.6139));

        ride = new RideEntity();
        ride.setId(UUID.fromString("93000000-0000-0000-0000-000000000001"));
        ride.setBookingRequest(rideRequest);
        ride.setLifecycleStatus(RideLifecycleStatus.SEARCHING_DRIVER);
    }

    @Test
    void shouldAssignBestDriverAndReserveAvailability() {
        NearbyDriverResult nearby = new NearbyDriverResult(
                driverProfile.getId(),
                vehicle.getId(),
                VehicleType.SEDAN,
                AvailabilityStatus.AVAILABLE,
                new BigDecimal("4.90"),
                false,
                null,
                28.6139,
                77.2090,
                400,
                Instant.parse("2026-04-18T11:59:50Z")
        );
        DriverDispatchCandidate candidate = new DriverDispatchCandidate(
                driverProfile.getId(),
                vehicle.getId(),
                VehicleType.SEDAN,
                (short) 4,
                AvailabilityStatus.AVAILABLE,
                OnlineStatus.ONLINE,
                null,
                new BigDecimal("4.90"),
                new BigDecimal("95.00"),
                new BigDecimal("3.00"),
                new BigDecimal("4.00"),
                false,
                Instant.parse("2026-04-18T11:40:00Z"),
                Instant.parse("2026-04-18T11:59:50Z")
        );
        DriverAssignmentCandidateScore score = new DriverAssignmentCandidateScore(
                driverProfile.getId(),
                vehicle.getId(),
                0.91,
                75,
                400,
                new BigDecimal("4.90"),
                new BigDecimal("95.00"),
                new BigDecimal("3.00"),
                1200,
                new BigDecimal("4.00"),
                0.2, 0.2, 0.1, 0.1, 0.1, 0.1, 0.03, 0.02, 0.02,
                candidate
        );
        DriverAvailabilityEntity availability = new DriverAvailabilityEntity();
        availability.setId(UUID.fromString("91000000-0000-0000-0000-000000000001"));
        availability.setDriverProfile(driverProfile);
        availability.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        availability.setOnlineStatus(OnlineStatus.ONLINE);

        when(rideRequestJpaRepository.findByIdForUpdate(rideRequest.getId())).thenReturn(Optional.of(rideRequest));
        when(rideJpaRepository.findByBookingRequestIdForUpdate(rideRequest.getId())).thenReturn(Optional.of(ride));
        when(driverAssignmentAttemptJpaRepository.findByRideRequestIdOrderByAttemptNoAsc(rideRequest.getId())).thenReturn(List.of());
        when(driverAvailabilityService.searchNearby(any())).thenReturn(List.of(nearby));
        when(driverProfileJpaRepository.findDispatchCandidates(any())).thenReturn(List.of(candidate));
        when(driverAssignmentScoreCalculator.score(candidate, 400)).thenReturn(score);
        when(driverProfileJpaRepository.getReferenceById(driverProfile.getId())).thenReturn(driverProfile);
        when(driverAvailabilityJpaRepository.findByDriverProfileIdForUpdate(driverProfile.getId())).thenReturn(Optional.of(availability));

        Optional<DriverAssignmentResult> result = driverAssignmentService.assignBestDriver(new DriverAssignmentRequest(
                rideRequest.getId(),
                AssignmentTrigger.BOOKING_REQUESTED
        ));

        assertThat(result).isPresent();
        assertThat(result.get().driverProfileId()).isEqualTo(driverProfile.getId());
        assertThat(availability.getCurrentRideId()).isEqualTo(ride.getId());
        assertThat(rideRequest.getRequestStatus()).isEqualTo(RideRequestStatusEntityType.DRIVER_ASSIGNED);
        assertThat(ride.getLifecycleStatus()).isEqualTo(RideLifecycleStatus.DRIVER_ASSIGNED);
        verify(rideEventPublisher).publish(eq(RideEventType.DRIVER_ASSIGNED), eq(rideRequest.getId()), any());
    }

    @Test
    void shouldReleaseDriverAndReturnRideToSearchingOnTimeout() {
        DriverAvailabilityEntity availability = new DriverAvailabilityEntity();
        availability.setId(UUID.fromString("91000000-0000-0000-0000-000000000001"));
        availability.setDriverProfile(driverProfile);
        availability.setAvailabilityStatus(AvailabilityStatus.BUSY);
        availability.setOnlineStatus(OnlineStatus.ONLINE);
        availability.setCurrentRideId(ride.getId());

        DriverAssignmentAttemptEntity attempt = new DriverAssignmentAttemptEntity();
        attempt.setId(UUID.fromString("92000000-0000-0000-0000-000000000001"));
        attempt.setRideRequest(rideRequest);
        attempt.setRide(ride);
        attempt.setDriverProfile(driverProfile);
        attempt.setVehicle(vehicle);
        attempt.setDispatchRound(1);
        attempt.setAttemptNo(1);
        attempt.setAssignmentStatus(DriverAssignmentAttemptStatus.PENDING_DRIVER_RESPONSE);
        attempt.setExpiresAt(Instant.parse("2026-04-18T11:59:59Z"));

        rideRequest.setRequestStatus(RideRequestStatusEntityType.DRIVER_ASSIGNED);
        ride.setLifecycleStatus(RideLifecycleStatus.DRIVER_ASSIGNED);
        ride.setDriverProfile(driverProfile);
        ride.setVehicle(vehicle);
        ride.setAssignedAt(Instant.parse("2026-04-18T11:59:40Z"));

        when(driverAssignmentAttemptJpaRepository.findByIdForUpdate(attempt.getId())).thenReturn(Optional.of(attempt));
        when(rideRequestJpaRepository.findByIdForUpdate(rideRequest.getId())).thenReturn(Optional.of(rideRequest));
        when(rideJpaRepository.findByBookingRequestIdForUpdate(rideRequest.getId())).thenReturn(Optional.of(ride));
        when(driverAvailabilityJpaRepository.findByDriverProfileIdForUpdate(driverProfile.getId())).thenReturn(Optional.of(availability));

        Optional<UUID> result = driverAssignmentService.expirePendingAssignment(attempt.getId());

        assertThat(result).contains(rideRequest.getId());
        assertThat(attempt.getAssignmentStatus()).isEqualTo(DriverAssignmentAttemptStatus.TIMED_OUT);
        assertThat(availability.getCurrentRideId()).isNull();
        assertThat(availability.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        assertThat(rideRequest.getRequestStatus()).isEqualTo(RideRequestStatusEntityType.SEARCHING_DRIVER);
        assertThat(ride.getLifecycleStatus()).isEqualTo(RideLifecycleStatus.SEARCHING_DRIVER);
        verify(rideEventPublisher).publish(eq(RideEventType.DRIVER_ASSIGNMENT_TIMED_OUT), eq(rideRequest.getId()), any());
    }

    private Point point(double longitude, double latitude) {
        return new GeometryFactory().createPoint(new Coordinate(longitude, latitude));
    }
}
