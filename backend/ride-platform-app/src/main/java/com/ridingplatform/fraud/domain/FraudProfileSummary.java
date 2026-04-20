package com.ridingplatform.fraud.domain;

import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudProfileSummary(
        UUID profileId,
        FraudSubjectType subjectType,
        UUID subjectId,
        BigDecimal aggregateScore,
        RiskLevel riskLevel,
        int activeFlagCount,
        boolean derivedBlocked,
        boolean manualBlocked,
        boolean blocked,
        String blockedReason,
        Instant lastSignalAt,
        Instant lastAssessedAt
) {
}
