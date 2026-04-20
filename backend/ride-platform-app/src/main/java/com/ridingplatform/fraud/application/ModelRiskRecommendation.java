package com.ridingplatform.fraud.application;

import com.ridingplatform.fraud.domain.RiskLevel;
import java.math.BigDecimal;
import java.util.Map;

public record ModelRiskRecommendation(
        String modelName,
        BigDecimal score,
        RiskLevel level,
        boolean blockRecommended,
        Map<String, Object> evidence
) {
}
