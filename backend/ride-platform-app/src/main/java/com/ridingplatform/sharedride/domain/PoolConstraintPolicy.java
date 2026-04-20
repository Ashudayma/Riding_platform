package com.ridingplatform.sharedride.domain;

public record PoolConstraintPolicy(
        int maxDetourMinutes,
        int maxPickupStops,
        int maxDropStops,
        int maxPickupWaitMinutes,
        int maxDetourPercentage,
        int maxPoolSearchRadiusMeters,
        double baseCompatibilityThreshold
) {
}
