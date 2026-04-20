package com.ridingplatform.ride.application;

import java.util.UUID;

public record RideStopResult(
        UUID stopId,
        String stopType,
        int requestSequenceNo,
        Integer rideSequenceNo,
        String address
) {
}
