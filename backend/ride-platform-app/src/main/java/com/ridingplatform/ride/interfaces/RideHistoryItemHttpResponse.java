package com.ridingplatform.ride.interfaces;

import com.ridingplatform.ride.application.RideBookingStatus;
import com.ridingplatform.ride.domain.RideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RideHistoryItemHttpResponse(
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
