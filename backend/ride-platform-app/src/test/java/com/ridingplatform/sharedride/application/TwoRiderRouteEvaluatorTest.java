package com.ridingplatform.sharedride.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.ridingplatform.ride.infrastructure.persistence.StopType;
import com.ridingplatform.sharedride.domain.PoolConstraintPolicy;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TwoRiderRouteEvaluatorTest {

    @Test
    void shouldEvaluateAllTwoRiderPermutationsAndReturnFeasiblePlans() {
        TwoRiderRouteEvaluator evaluator = new TwoRiderRouteEvaluator();
        PoolRouteEvaluationContext context = new PoolRouteEvaluationContext(
                new PoolConstraintPolicy(20, 3, 3, 12, 50, 5000, 0.5d),
                (short) 4,
                (short) 1,
                stop(RouteStopRole.PICKUP_EXISTING, StopType.PICKUP, 28.6139, 77.2090),
                stop(RouteStopRole.DROPOFF_EXISTING, StopType.DROPOFF, 28.6200, 77.2200),
                stop(RouteStopRole.PICKUP_NEW, StopType.PICKUP, 28.6150, 77.2140),
                stop(RouteStopRole.DROPOFF_NEW, StopType.DROPOFF, 28.6250, 77.2280)
        );

        List<PoolRoutePlan> plans = evaluator.evaluate(context);

        assertThat(plans).isNotEmpty();
        assertThat(plans).allMatch(plan -> plan.peakSeatUsage() <= 4);
        assertThat(plans.stream().map(PoolRoutePlan::permutationCode))
                .contains("PA->PB->DA->DB")
                .allMatch(code -> List.of("PA->PB->DA->DB", "PA->PB->DB->DA", "PB->PA->DA->DB", "PB->PA->DB->DA").contains(code));
    }

    @Test
    void shouldRejectPlansThatExceedSeatCapacity() {
        TwoRiderRouteEvaluator evaluator = new TwoRiderRouteEvaluator();
        PoolRouteEvaluationContext context = new PoolRouteEvaluationContext(
                new PoolConstraintPolicy(20, 3, 3, 12, 50, 5000, 0.5d),
                (short) 1,
                (short) 1,
                stop(RouteStopRole.PICKUP_EXISTING, StopType.PICKUP, 28.6139, 77.2090),
                stop(RouteStopRole.DROPOFF_EXISTING, StopType.DROPOFF, 28.6200, 77.2200),
                stop(RouteStopRole.PICKUP_NEW, StopType.PICKUP, 28.6150, 77.2140),
                stop(RouteStopRole.DROPOFF_NEW, StopType.DROPOFF, 28.6250, 77.2280)
        );

        List<PoolRoutePlan> plans = evaluator.evaluate(context);

        assertThat(plans).isEmpty();
    }

    private PoolRouteStop stop(RouteStopRole role, StopType stopType, double latitude, double longitude) {
        return new PoolRouteStop(
                UUID.randomUUID(),
                UUID.randomUUID(),
                stopType,
                role,
                latitude,
                longitude,
                "Address",
                "Locality",
                (short) 1
        );
    }
}
