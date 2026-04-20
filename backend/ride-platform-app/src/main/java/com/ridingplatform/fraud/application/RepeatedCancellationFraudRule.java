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
public class RepeatedCancellationFraudRule extends AbstractFraudRule implements FraudRule {

    @Override
    public Set<FraudSignalType> supports() {
        return Set.of(FraudSignalType.RIDE_CANCELLED);
    }

    @Override
    public List<FraudRuleMatch> evaluate(FraudRuleContext context) {
        long count = context.windowCounterService().increment(
                "fraud:cancel:" + context.signal().subjectType() + ":" + context.signal().subjectId(),
                Duration.ofHours(context.properties().cancellationWindowHours())
        );
        if (count < context.properties().repeatedCancellationThreshold()) {
            return List.of();
        }
        RiskLevel severity = count >= (context.properties().repeatedCancellationThreshold() * 2L) ? RiskLevel.CRITICAL : RiskLevel.HIGH;
        BigDecimal score = severity == RiskLevel.CRITICAL ? new BigDecimal("40.0000") : new BigDecimal("22.0000");
        return List.of(match(
                FraudRuleCode.REPEATED_CANCELLATIONS,
                severity,
                score,
                "Repeated cancellations detected",
                "The account crossed the configured cancellation threshold within the active review window.",
                severity == RiskLevel.CRITICAL,
                Map.of("cancellationCount", count, "windowHours", context.properties().cancellationWindowHours())
        ));
    }
}
