package com.ridingplatform.sharedride.application;

import com.ridingplatform.sharedride.domain.PoolConstraintPolicy;

public record PoolRouteEvaluationContext(
        PoolConstraintPolicy policy,
        short seatCapacity,
        short occupiedSeatCount,
        PoolRouteStop existingPickup,
        PoolRouteStop existingDrop,
        PoolRouteStop newPickup,
        PoolRouteStop newDrop
) {
}
