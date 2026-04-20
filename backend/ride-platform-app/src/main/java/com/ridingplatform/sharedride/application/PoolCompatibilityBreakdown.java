package com.ridingplatform.sharedride.application;

public record PoolCompatibilityBreakdown(
        double routeOverlapScore,
        double waitTimeScore,
        double detourScore,
        double seatUtilizationScore,
        double riderCompatibilityScore
) {
}
