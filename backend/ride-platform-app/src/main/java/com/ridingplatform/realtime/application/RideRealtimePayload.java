package com.ridingplatform.realtime.application;

import java.time.Instant;
import java.util.UUID;

public record RideRealtimePayload(
        String eventType,
        UUID rideRequestId,
        UUID rideId,
        String status,
        String message,
        Instant occurredAt
) {
}
