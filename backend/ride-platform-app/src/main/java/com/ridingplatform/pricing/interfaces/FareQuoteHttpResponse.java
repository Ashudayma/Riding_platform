package com.ridingplatform.pricing.interfaces;

import com.ridingplatform.ride.domain.RideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FareQuoteHttpResponse(
        UUID fareQuoteId,
        RideType rideType,
        String cityCode,
        String zoneCode,
        Integer pricingVersion,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal durationFare,
        BigDecimal surgeMultiplier,
        BigDecimal surgeAmount,
        BigDecimal bookingFee,
        BigDecimal waitingCharge,
        BigDecimal cancellationCharge,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal poolingDiscountAmount,
        BigDecimal totalAmount,
        String currencyCode,
        int distanceMeters,
        int durationSeconds,
        Instant expiresAt,
        Instant finalizedAt
) {
}
