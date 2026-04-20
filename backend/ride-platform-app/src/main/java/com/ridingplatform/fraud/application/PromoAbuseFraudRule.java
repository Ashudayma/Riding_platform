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
public class PromoAbuseFraudRule extends AbstractFraudRule implements FraudRule {

    @Override
    public Set<FraudSignalType> supports() {
        return Set.of(FraudSignalType.PROMO_REDEEMED);
    }

    @Override
    public List<FraudRuleMatch> evaluate(FraudRuleContext context) {
        long count = context.windowCounterService().increment(
                "fraud:promo:" + context.signal().subjectType() + ":" + context.signal().subjectId(),
                Duration.ofHours(context.properties().promoWindowHours())
        );
        if (count < context.properties().promoAbuseThreshold()) {
            return List.of();
        }
        return List.of(match(
                FraudRuleCode.PROMO_ABUSE,
                RiskLevel.HIGH,
                new BigDecimal("18.0000"),
                "Promo abuse threshold exceeded",
                "Promo redemption frequency crossed the abuse threshold for the active rolling window.",
                false,
                Map.of("promoRedemptionCount", count)
        ));
    }
}
