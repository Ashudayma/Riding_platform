package com.ridingplatform.sharedride.interfaces;

import com.ridingplatform.ride.infrastructure.persistence.StopType;
import com.ridingplatform.sharedride.application.RouteStopRole;
import java.util.UUID;

public record SharedRideRouteStopHttpResponse(
        UUID rideRequestId,
        UUID riderProfileId,
        StopType stopType,
        RouteStopRole role,
        int sequenceNo,
        String address
) {
}
