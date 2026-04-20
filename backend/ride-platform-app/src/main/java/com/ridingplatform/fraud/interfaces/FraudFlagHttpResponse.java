package com.ridingplatform.fraud.interfaces;

import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudFlagHttpResponse(
        UUID flagId,
        FraudSubjectType subjectType,
        UUID subjectId,
        String severity,
        FraudFlagStatus flagStatus,
        String ruleCode,
        BigDecimal riskScore,
        String title,
        String description,
        Instant createdAt,
        Instant resolvedAt
) {
}
