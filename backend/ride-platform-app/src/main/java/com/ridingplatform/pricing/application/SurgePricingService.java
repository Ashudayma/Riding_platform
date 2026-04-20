package com.ridingplatform.pricing.application;

import java.math.BigDecimal;

public interface SurgePricingService {

    BigDecimal surgeMultiplier(PricingRequestContext context);
}
