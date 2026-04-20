package com.ridingplatform.ride.infrastructure.persistence;

public enum DriverAssignmentAttemptStatus {
    PENDING_DRIVER_RESPONSE,
    ACCEPTED,
    REJECTED,
    TIMED_OUT,
    FAILED,
    SKIPPED
}
