package com.ridingplatform.admin.application;

import java.time.Instant;
import java.util.UUID;

public record AdminRideView(
        UUID rideRequestId,
        UUID rideId,
        UUID riderProfileId,
        UUID driverProfileId,
        String rideType,
        String requestStatus,
        String lifecycleStatus,
        String originAddress,
        String destinationAddress,
        Instant requestedAt,
        Instant assignedAt,
        Instant completedAt
) {
}
