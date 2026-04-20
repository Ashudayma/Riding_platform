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
public class AccountDeviceAnomalyFraudRule extends AbstractFraudRule implements FraudRule {

    @Override
    public Set<FraudSignalType> supports() {
        return Set.of(FraudSignalType.ACCOUNT_DEVICE_ANOMALY);
    }

    @Override
    public List<FraudRuleMatch> evaluate(FraudRuleContext context) {
        String deviceFingerprint = String.valueOf(context.signal().attributes().getOrDefault("deviceFingerprint", "unknown"));
        long count = context.windowCounterService().increment(
                "fraud:device:" + deviceFingerprint,
                Duration.ofHours(context.properties().accountAnomalyWindowHours())
        );
        if (count < 3) {
            return List.of();
        }
        return List.of(match(
                FraudRuleCode.ACCOUNT_DEVICE_ANOMALY,
                RiskLevel.HIGH,
                new BigDecimal("24.0000"),
                "Account-device anomaly detected",
                "Multiple risky actions are sharing the same device fingerprint inside the anomaly window.",
                true,
                Map.of("deviceFingerprint", deviceFingerprint, "deviceReuseCount", count)
        ));
    }
}
