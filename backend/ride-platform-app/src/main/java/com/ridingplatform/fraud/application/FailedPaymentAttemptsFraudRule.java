package com.ridingplatform.fraud.application;

import com.ridingplatform.fraud.domain.FraudRuleCode;
import com.ridingplatform.fraud.domain.FraudRuleMatch;
import com.ridingplatform.fraud.domain.FraudSignalType;
import com.ridingplatform.fraud.domain.RiskLevel;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FailedPaymentAttemptsFraudRule extends AbstractFraudRule implements FraudRule {

    @Override
    public Set<FraudSignalType> supports() {
        return Set.of(FraudSignalType.PAYMENT_FAILED);
    }

    @Override
    public List<FraudRuleMatch> evaluate(FraudRuleContext context) {
        long count = context.windowCounterService().increment(
                "fraud:payment-failed:" + context.signal().subjectType() + ":" + context.signal().subjectId(),
                Duration.ofHours(context.properties().paymentWindowHours())
        );
        if (count < context.properties().failedPaymentThreshold()) {
            return List.of();
        }
        return List.of(match(
                FraudRuleCode.FAILED_PAYMENT_ATTEMPTS,
                count >= context.properties().failedPaymentThreshold() + 2L ? RiskLevel.CRITICAL : RiskLevel.HIGH,
                new BigDecimal("28.0000"),
                "Too many failed payment attempts",
                "Failed payment attempts crossed the configured threshold for the current rolling window.",
                count >= context.properties().failedPaymentThreshold() + 2L,
                Map.of("failedPaymentCount", count, "windowHours", context.properties().paymentWindowHours())
        ));
    }
}
