package com.ridingplatform.ride.domain;

public record LocationPoint(
        double latitude,
        double longitude,
        String address
) {
}
