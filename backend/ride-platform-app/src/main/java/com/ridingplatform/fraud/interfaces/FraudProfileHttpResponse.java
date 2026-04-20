package com.ridingplatform.fraud.interfaces;

import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudProfileHttpResponse(
        UUID profileId,
        FraudSubjectType subjectType,
        UUID subjectId,
        BigDecimal aggregateScore,
        String riskLevel,
        int activeFlagCount,
        boolean derivedBlocked,
        boolean manualBlocked,
        boolean blocked,
        String blockedReason,
        Instant lastSignalAt,
        Instant lastAssessedAt
) {
}
