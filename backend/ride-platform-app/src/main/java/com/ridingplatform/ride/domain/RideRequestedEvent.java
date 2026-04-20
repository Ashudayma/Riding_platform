package com.ridingplatform.ride.domain;

import com.ridingplatform.common.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record RideRequestedEvent(
        UUID eventId,
        UUID rideRequestId,
        UUID riderId,
        RideType rideType,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ride.requested";
    }
}
