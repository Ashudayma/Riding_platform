package com.ridingplatform.sharedride.interfaces;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SharedRidePreviewHttpResponse(
        boolean matched,
        UUID sharedRideGroupId,
        UUID anchorRideRequestId,
        UUID anchorRideId,
        BigDecimal compatibilityScore,
        BigDecimal estimatedSavingsAmount,
        String rejectionReason,
        String permutationCode,
        Integer totalDurationSeconds,
        Integer totalDistanceMeters,
        List<SharedRideRouteStopHttpResponse> routeStops
) {
}
