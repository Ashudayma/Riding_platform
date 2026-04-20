package com.ridingplatform.sharedride.application;

import java.math.BigDecimal;
import java.util.UUID;

public record PoolMatchDecision(
        boolean matched,
        UUID sharedRideGroupId,
        UUID anchorRideRequestId,
        UUID anchorRideId,
        BigDecimal compatibilityScore,
        BigDecimal estimatedSavingsAmount,
        String rejectionReason,
        PoolRoutePlan routePlan,
        PoolCompatibilityBreakdown compatibilityBreakdown
) {
}
