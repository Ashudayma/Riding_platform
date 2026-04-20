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
public class GpsSpoofingFraudRule extends AbstractFraudRule implements FraudRule {

    @Override
    public Set<FraudSignalType> supports() {
        return Set.of(FraudSignalType.DRIVER_LOCATION_UPDATE);
    }

    @Override
    public List<FraudRuleMatch> evaluate(FraudRuleContext context) {
        BigDecimal accuracy = decimal(context.signal().attributes().get("accuracyMeters"));
        BigDecimal speed = decimal(context.signal().attributes().get("speedKph"));
        boolean suspiciousAccuracy = accuracy != null && accuracy.intValue() >= context.properties().gpsHighAccuracyMeters();
        boolean impossibleSpeed = speed != null && speed.intValue() >= context.properties().impossibleSpeedKph();
        if (!suspiciousAccuracy && !impossibleSpeed) {
            return List.of();
        }
        return List.of(match(
                FraudRuleCode.GPS_SPOOFING,
                impossibleSpeed ? RiskLevel.CRITICAL : RiskLevel.HIGH,
                impossibleSpeed ? new BigDecimal("45.0000") : new BigDecimal("24.0000"),
                "GPS spoofing indicators detected",
                "Driver location telemetry contains impossible speed or poor accuracy patterns associated with spoofing.",
                impossibleSpeed,
                Map.of(
                        "accuracyMeters", accuracy == null ? "" : accuracy.toPlainString(),
                        "speedKph", speed == null ? "" : speed.toPlainString(),
                        "accuracyThreshold", context.properties().gpsHighAccuracyMeters(),
                        "speedThreshold", context.properties().impossibleSpeedKph()
                )
        ));
    }

    private BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }
}
