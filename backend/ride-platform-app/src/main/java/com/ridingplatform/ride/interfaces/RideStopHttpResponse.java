package com.ridingplatform.ride.interfaces;

import java.util.UUID;

public record RideStopHttpResponse(
        UUID stopId,
        String stopType,
        int requestSequenceNo,
        Integer rideSequenceNo,
        String address
) {
}
