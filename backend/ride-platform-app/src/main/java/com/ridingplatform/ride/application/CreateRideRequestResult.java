package com.ridingplatform.ride.application;

import com.ridingplatform.ride.domain.RideStatus;
import com.ridingplatform.ride.domain.RideType;
import java.util.UUID;

public record CreateRideRequestResult(
        UUID rideRequestId,
        UUID riderId,
        RideType rideType,
        RideStatus status
) {
}
