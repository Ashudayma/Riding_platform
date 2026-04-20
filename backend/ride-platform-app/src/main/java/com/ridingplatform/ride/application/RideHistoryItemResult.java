package com.ridingplatform.ride.application;

import com.ridingplatform.ride.domain.RideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RideHistoryItemResult(
        UUID rideRequestId,
        UUID rideId,
        RideType rideType,
        RideBookingStatus status,
        String pickupAddress,
        String dropAddress,
        BigDecimal amount,
        String currencyCode,
        Instant requestedAt
) {
}
