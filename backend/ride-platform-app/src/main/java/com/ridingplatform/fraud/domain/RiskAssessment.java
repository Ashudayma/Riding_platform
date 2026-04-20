package com.ridingplatform.fraud.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RiskAssessment(
        UUID assessmentId,
        String subjectType,
        UUID subjectId,
        BigDecimal score,
        RiskLevel riskLevel,
        List<String> triggeredSignals,
        Instant assessedAt
) {
}
