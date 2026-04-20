package com.ridingplatform.ride.infrastructure.persistence;

public enum DriverAssignmentFailureReason {
    DRIVER_NOT_AVAILABLE,
    DRIVER_OFFLINE,
    VEHICLE_INCOMPATIBLE,
    RISK_BLOCKED,
    RACE_LOST,
    NO_CANDIDATES_AVAILABLE,
    DRIVER_REJECTED,
    DRIVER_RESPONSE_TIMEOUT,
    MAX_DISPATCH_ROUNDS_EXCEEDED
}
