package com.ridingplatform.fraud.domain;

import java.math.BigDecimal;
import java.util.Map;

public record FraudRuleMatch(
        String ruleCode,
        RiskLevel severity,
        BigDecimal score,
        String title,
        String description,
        boolean blockRecommended,
        Map<String, Object> evidence
) {
}
