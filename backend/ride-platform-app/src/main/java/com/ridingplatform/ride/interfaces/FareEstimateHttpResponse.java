package com.ridingplatform.ride.interfaces;

import com.ridingplatform.ride.domain.RideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FareEstimateHttpResponse(
        UUID fareQuoteId,
        RideType rideType,
        BigDecimal totalAmount,
        String currencyCode,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal durationFare,
        BigDecimal surgeMultiplier,
        BigDecimal bookingFee,
        BigDecimal taxAmount,
        BigDecimal poolingDiscountAmount,
        int quotedDistanceMeters,
        int quotedDurationSeconds,
        Instant expiresAt
) {
}
