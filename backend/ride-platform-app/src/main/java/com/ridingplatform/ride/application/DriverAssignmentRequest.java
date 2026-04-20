package com.ridingplatform.ride.application;

import java.util.UUID;

public record DriverAssignmentRequest(
        UUID rideRequestId,
        AssignmentTrigger trigger
) {
}
