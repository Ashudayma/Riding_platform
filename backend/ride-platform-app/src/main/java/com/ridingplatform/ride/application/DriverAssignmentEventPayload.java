package com.ridingplatform.ride.application;

import java.time.Instant;
import java.util.UUID;

public record DriverAssignmentEventPayload(
        UUID rideRequestId,
        UUID rideId,
        UUID driverProfileId,
        UUID vehicleId,
        String assignmentStatus,
        String failureReason,
        double weightedScore,
        double distanceMeters,
        int etaSeconds,
        int dispatchRound,
        int attemptNo,
        Instant occurredAt
) {
}
