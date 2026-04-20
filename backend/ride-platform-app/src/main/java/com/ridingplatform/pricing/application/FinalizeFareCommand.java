package com.ridingplatform.pricing.application;

import java.util.UUID;

public record FinalizeFareCommand(
        UUID fareQuoteId,
        int actualDistanceMeters,
        int actualDurationSeconds,
        int waitingDurationSeconds,
        boolean cancelled,
        int cancelledAfterDistanceMeters
) {
}
