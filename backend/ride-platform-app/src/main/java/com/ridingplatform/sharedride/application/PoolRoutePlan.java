package com.ridingplatform.sharedride.application;

import java.util.List;

public record PoolRoutePlan(
        String permutationCode,
        List<PoolRouteStop> orderedStops,
        int totalDistanceMeters,
        int totalDurationSeconds,
        int existingRiderPickupWaitSeconds,
        int newRiderPickupWaitSeconds,
        int existingRiderDetourSeconds,
        int newRiderDetourSeconds,
        int peakSeatUsage,
        double detourPercentageExisting,
        double detourPercentageNew
) {
}
