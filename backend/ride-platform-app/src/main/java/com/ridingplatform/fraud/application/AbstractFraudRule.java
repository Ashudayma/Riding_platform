package com.ridingplatform.fraud.application;

import com.ridingplatform.fraud.domain.FraudRuleMatch;
import com.ridingplatform.fraud.domain.RiskLevel;
import java.math.BigDecimal;
import java.util.Map;

abstract class AbstractFraudRule {

    protected FraudRuleMatch match(
            String ruleCode,
            RiskLevel severity,
            BigDecimal score,
            String title,
            String description,
            boolean blockRecommended,
            Map<String, Object> evidence
    ) {
        return new FraudRuleMatch(ruleCode, severity, score, title, description, blockRecommended, evidence);
    }
}
