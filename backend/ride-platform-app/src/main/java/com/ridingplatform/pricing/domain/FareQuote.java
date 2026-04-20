package com.ridingplatform.pricing.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record FareQuote(
        UUID quoteId,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal timeFare,
        BigDecimal surgeMultiplier,
        BigDecimal poolingDiscount,
        BigDecimal finalAmount
) {
}
