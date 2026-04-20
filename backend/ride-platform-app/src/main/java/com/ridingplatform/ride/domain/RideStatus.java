package com.ridingplatform.ride.domain;

public enum RideStatus {
    REQUESTED,
    SEARCHING_DRIVER,
    DRIVER_ASSIGNED,
    DRIVER_EN_ROUTE,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
