package com.ridingplatform.ride.application;

import com.ridingplatform.ride.domain.RideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FareEstimateResult(
        UUID fareQuoteId,
        RideType rideType,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal durationFare,
        BigDecimal surgeMultiplier,
        BigDecimal bookingFee,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal poolingDiscountAmount,
        BigDecimal totalAmount,
        String currencyCode,
        int quotedDistanceMeters,
        int quotedDurationSeconds,
        Instant expiresAt
) {
}
