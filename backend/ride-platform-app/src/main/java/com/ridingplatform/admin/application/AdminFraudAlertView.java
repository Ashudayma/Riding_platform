package com.ridingplatform.admin.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AdminFraudAlertView(
        UUID flagId,
        String subjectType,
        UUID subjectId,
        String severity,
        String flagStatus,
        String ruleCode,
        BigDecimal riskScore,
        String title,
        Instant createdAt
) {
}
