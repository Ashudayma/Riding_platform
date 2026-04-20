package com.ridingplatform.pricing.interfaces;

import com.ridingplatform.ride.domain.RideType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FinalizeFareHttpRequest(
        @NotNull RideType rideType,
        @Min(0) int actualDistanceMeters,
        @Min(0) int actualDurationSeconds,
        @Min(0) int waitingDurationSeconds,
        boolean cancelled,
        @Min(0) int cancelledAfterDistanceMeters
) {
}
