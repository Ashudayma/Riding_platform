package com.ridingplatform.ride.infrastructure.persistence;

public enum RideRequestStatusEntityType {
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
