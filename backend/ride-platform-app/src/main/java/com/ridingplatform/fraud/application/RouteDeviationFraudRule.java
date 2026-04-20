package com.ridingplatform.fraud.application;

import com.ridingplatform.fraud.domain.FraudRuleCode;
import com.ridingplatform.fraud.domain.FraudRuleMatch;
import com.ridingplatform.fraud.domain.FraudSignalType;
import com.ridingplatform.fraud.domain.RiskLevel;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RouteDeviationFraudRule extends AbstractFraudRule implements FraudRule {

    @Override
    public Set<FraudSignalType> supports() {
        return Set.of(FraudSignalType.ROUTE_DEVIATION, FraudSignalType.RIDE_COMPLETED);
    }

    @Override
    public List<FraudRuleMatch> evaluate(FraudRuleContext context) {
        Integer deviationPercent = value(context.signal().attributes().get("routeDeviationPercent"));
        if (deviationPercent == null || deviationPercent < context.properties().routeDeviationThresholdPercent()) {
            return List.of();
        }
        return List.of(match(
                FraudRuleCode.ROUTE_DEVIATION,
                deviationPercent >= context.properties().routeDeviationThresholdPercent() + 20 ? RiskLevel.CRITICAL : RiskLevel.HIGH,
                new BigDecimal("26.0000"),
                "Abnormal route deviation detected",
                "Observed route deviation exceeded the city safety threshold configured for fraud review.",
                deviationPercent >= context.properties().routeDeviationThresholdPercent() + 20,
                Map.of("routeDeviationPercent", deviationPercent)
        ));
    }

    private Integer value(Object input) {
        return input == null ? null : Integer.parseInt(String.valueOf(input));
    }
}
