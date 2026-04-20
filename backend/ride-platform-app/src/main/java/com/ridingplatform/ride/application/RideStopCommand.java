package com.ridingplatform.ride.application;

public record RideStopCommand(
        StopTypeCommand stopType,
        double latitude,
        double longitude,
        String address,
        String locality
) {
}
