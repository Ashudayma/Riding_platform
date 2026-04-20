package com.ridingplatform.sharedride.application;

import com.ridingplatform.ride.infrastructure.persistence.StopType;
import java.util.UUID;

public record PoolRouteStop(
        UUID rideRequestId,
        UUID riderProfileId,
        StopType stopType,
        RouteStopRole role,
        double latitude,
        double longitude,
        String address,
        String locality,
        short passengerCount
) {
}
