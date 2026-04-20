package com.ridingplatform.admin.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminPricingRuleView(
        UUID pricingRuleId,
        String cityCode,
        String zoneCode,
        String rideType,
        String vehicleType,
        int pricingVersion,
        boolean active,
        BigDecimal baseFare,
        BigDecimal perKmRate,
        BigDecimal perMinuteRate,
        BigDecimal sharedDiscountFactor,
        Instant effectiveFrom,
        Instant effectiveTo
) {
}
