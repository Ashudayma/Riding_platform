package com.ridingplatform.pricing.application;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingRuleSnapshot(
        UUID pricingRuleSetId,
        String cityCode,
        String zoneCode,
        int pricingVersion,
        String currencyCode,
        BigDecimal baseFare,
        BigDecimal minimumFare,
        BigDecimal bookingFee,
        BigDecimal perKmRate,
        BigDecimal perMinuteRate,
        BigDecimal perStopCharge,
        BigDecimal waitingChargePerMinute,
        BigDecimal cancellationBaseCharge,
        BigDecimal cancellationPerKmCharge,
        BigDecimal sharedDiscountFactor,
        BigDecimal taxPercentage,
        BigDecimal surgeCapMultiplier,
        BigDecimal nightSurchargePercentage,
        BigDecimal airportSurchargeAmount
) {
}
