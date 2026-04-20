package com.ridingplatform.realtime.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ridingplatform.driver.application.DriverLocationEvent;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.application.DriverAssignmentEventPayload;
import com.ridingplatform.ride.application.RideEventType;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.tracking.infrastructure.persistence.LocationProviderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RideRealtimeGatewayTest {

    private final RideJpaRepository rideJpaRepository = Mockito.mock(RideJpaRepository.class);
    private final RealtimeMessagingService realtimeMessagingService = Mockito.mock(RealtimeMessagingService.class);
    private final DriverLocationFanoutGuard driverLocationFanoutGuard = Mockito.mock(DriverLocationFanoutGuard.class);

    private RideRealtimeGateway rideRealtimeGateway;

    @BeforeEach
    void setUp() {
        rideRealtimeGateway = new RideRealtimeGateway(
                rideJpaRepository,
                realtimeMessagingService,
                driverLocationFanoutGuard
        );
    }

    @Test
    void shouldSendRideEventToRiderAndDriverQueues() {
        UUID rideRequestId = UUID.fromString("90000000-0000-0000-0000-000000000001");
        when(rideJpaRepository.findRealtimeAudienceByRideRequestId(rideRequestId)).thenReturn(Optional.of(
                new RideRealtimeAudience(
                        rideRequestId,
                        UUID.fromString("93000000-0000-0000-0000-000000000001"),
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        UUID.fromString("20000000-0000-0000-0000-000000000001")
                )
        ));

        rideRealtimeGateway.publishRideEvent(RideEventType.BOOKING_REQUESTED, rideRequestId, "SEARCHING_DRIVER", "Ride request created");

        verify(realtimeMessagingService).sendToUser(eq("10000000-0000-0000-0000-000000000001"), eq(RealtimeDestination.RIDER_RIDE_QUEUE), any(RideRealtimePayload.class));
        verify(realtimeMessagingService).sendToUser(eq("20000000-0000-0000-0000-000000000001"), eq(RealtimeDestination.DRIVER_RIDE_QUEUE), any(RideRealtimePayload.class));
    }

    @Test
    void shouldThrottleAndPublishDriverLocationToRider() {
        UUID rideId = UUID.fromString("93000000-0000-0000-0000-000000000001");
        when(driverLocationFanoutGuard.shouldPublish(rideId, 28.6139, 77.2090)).thenReturn(true);
        when(rideJpaRepository.findRealtimeAudienceByRideId(rideId)).thenReturn(Optional.of(
                new RideRealtimeAudience(
                        UUID.fromString("90000000-0000-0000-0000-000000000001"),
                        rideId,
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        UUID.fromString("20000000-0000-0000-0000-000000000001")
                )
        ));

        rideRealtimeGateway.publishDriverLocation(new DriverLocationEvent(
                UUID.fromString("f0000000-0000-0000-0000-000000000001"),
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                rideId,
                VehicleType.SEDAN,
                28.6139,
                77.2090,
                new BigDecimal("180"),
                new BigDecimal("25"),
                new BigDecimal("5"),
                LocationProviderType.GPS,
                Instant.parse("2026-04-18T12:00:00Z")
        ));

        verify(realtimeMessagingService).sendToUser(eq("10000000-0000-0000-0000-000000000001"), eq(RealtimeDestination.RIDE_LOCATION_QUEUE), any(DriverLocationRealtimePayload.class));
    }

    @Test
    void shouldSendAssignmentEventToDriverAssignmentQueue() {
        UUID rideRequestId = UUID.fromString("90000000-0000-0000-0000-000000000001");
        when(rideJpaRepository.findRealtimeAudienceByRideRequestId(rideRequestId)).thenReturn(Optional.of(
                new RideRealtimeAudience(
                        rideRequestId,
                        UUID.fromString("93000000-0000-0000-0000-000000000001"),
                        UUID.fromString("10000000-0000-0000-0000-000000000001"),
                        UUID.fromString("20000000-0000-0000-0000-000000000001")
                )
        ));

        rideRealtimeGateway.publishAssignmentEvent(new DriverAssignmentEventPayload(
                rideRequestId,
                UUID.fromString("93000000-0000-0000-0000-000000000001"),
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                "PENDING_DRIVER_RESPONSE",
                null,
                0.92,
                400,
                75,
                1,
                1,
                Instant.parse("2026-04-18T12:00:00Z")
        ));

        verify(realtimeMessagingService).sendToUser(eq("20000000-0000-0000-0000-000000000001"), eq(RealtimeDestination.DRIVER_ASSIGNMENT_QUEUE), any(DriverAssignmentEventPayload.class));
    }
}
