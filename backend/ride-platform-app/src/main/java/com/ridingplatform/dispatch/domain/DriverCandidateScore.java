package com.ridingplatform.dispatch.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record DriverCandidateScore(
        UUID driverId,
        int distanceMeters,
        int etaSeconds,
        BigDecimal rating,
        boolean available,
        BigDecimal generatedScore
) {
}
