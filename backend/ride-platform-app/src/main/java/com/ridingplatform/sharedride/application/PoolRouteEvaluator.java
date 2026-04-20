package com.ridingplatform.sharedride.application;

import java.util.List;

public interface PoolRouteEvaluator {

    List<PoolRoutePlan> evaluate(PoolRouteEvaluationContext context);
}
