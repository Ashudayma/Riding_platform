package com.ridingplatform.ride.application;

import java.time.Instant;
import java.util.UUID;

public record DriverAssignmentResult(
        UUID rideRequestId,
        UUID rideId,
        UUID driverProfileId,
        UUID vehicleId,
        boolean assigned,
        int dispatchRound,
        int attemptNo,
        Instant expiresAt
) {
}
