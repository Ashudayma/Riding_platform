package com.ridingplatform.ride.application;

public enum RideBookingStatus {
    REQUESTED,
    SEARCHING_DRIVER,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVING,
    DRIVER_ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    FAILED
}
