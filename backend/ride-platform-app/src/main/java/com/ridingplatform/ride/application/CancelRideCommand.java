package com.ridingplatform.ride.application;

import java.util.UUID;

public record CancelRideCommand(
        UUID rideRequestId,
        UUID riderProfileId,
        String cancelReason
) {
}
