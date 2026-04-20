package com.ridingplatform.realtime.application;

import com.ridingplatform.driver.application.DriverLocationEvent;
import com.ridingplatform.ride.application.DriverAssignmentEventPayload;
import com.ridingplatform.ride.application.RideEventType;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RideRealtimeGateway {

    private final RideJpaRepository rideJpaRepository;
    private final RealtimeMessagingService realtimeMessagingService;
    private final DriverLocationFanoutGuard driverLocationFanoutGuard;

    public RideRealtimeGateway(
            RideJpaRepository rideJpaRepository,
            RealtimeMessagingService realtimeMessagingService,
            DriverLocationFanoutGuard driverLocationFanoutGuard
    ) {
        this.rideJpaRepository = rideJpaRepository;
        this.realtimeMessagingService = realtimeMessagingService;
        this.driverLocationFanoutGuard = driverLocationFanoutGuard;
    }

    public void publishRideEvent(RideEventType eventType, UUID rideRequestId, String status, String message) {
        rideJpaRepository.findRealtimeAudienceByRideRequestId(rideRequestId).ifPresent(audience -> {
            RideRealtimePayload payload = new RideRealtimePayload(
                    eventType.name(),
                    audience.rideRequestId(),
                    audience.rideId(),
                    status,
                    message,
                    Instant.now()
            );
            sendRidePayload(audience, payload);
        });
    }

    public void publishAssignmentEvent(DriverAssignmentEventPayload payload) {
        rideJpaRepository.findRealtimeAudienceByRideRequestId(payload.rideRequestId()).ifPresent(audience -> {
            RideRealtimePayload ridePayload = new RideRealtimePayload(
                    payload.assignmentStatus(),
                    audience.rideRequestId(),
                    audience.rideId(),
                    payload.assignmentStatus(),
                    payload.failureReason(),
                    payload.occurredAt()
            );
            sendRidePayload(audience, ridePayload);
            if (audience.driverSubject() != null) {
                realtimeMessagingService.sendToUser(audience.driverSubject().toString(), RealtimeDestination.DRIVER_ASSIGNMENT_QUEUE, payload);
            }
        });
    }

    public void publishDriverLocation(DriverLocationEvent event) {
        if (!driverLocationFanoutGuard.shouldPublish(event.currentRideId(), event.latitude(), event.longitude())) {
            return;
        }
        rideJpaRepository.findRealtimeAudienceByRideId(event.currentRideId()).ifPresent(audience -> {
            DriverLocationRealtimePayload payload = new DriverLocationRealtimePayload(
                    audience.rideId(),
                    event.driverProfileId(),
                    event.latitude(),
                    event.longitude(),
                    event.headingDegrees(),
                    event.speedKph(),
                    event.accuracyMeters(),
                    event.observedAt()
            );
            realtimeMessagingService.sendToUser(audience.riderSubject().toString(), RealtimeDestination.RIDE_LOCATION_QUEUE, payload);
            if (audience.driverSubject() != null) {
                realtimeMessagingService.sendToUser(audience.driverSubject().toString(), RealtimeDestination.DRIVER_RIDE_QUEUE, payload);
            }
        });
    }

    private void sendRidePayload(RideRealtimeAudience audience, RideRealtimePayload payload) {
        realtimeMessagingService.sendToUser(audience.riderSubject().toString(), RealtimeDestination.RIDER_RIDE_QUEUE, payload);
        if (audience.driverSubject() != null) {
            realtimeMessagingService.sendToUser(audience.driverSubject().toString(), RealtimeDestination.DRIVER_RIDE_QUEUE, payload);
        }
    }
}
