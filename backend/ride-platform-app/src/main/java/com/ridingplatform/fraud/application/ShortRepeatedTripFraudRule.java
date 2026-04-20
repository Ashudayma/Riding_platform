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
public class ShortRepeatedTripFraudRule extends AbstractFraudRule implements FraudRule {

    @Override
    public Set<FraudSignalType> supports() {
        return Set.of(FraudSignalType.RIDE_COMPLETED);
    }

    @Override
    public List<FraudRuleMatch> evaluate(FraudRuleContext context) {
        Integer distanceMeters = integer(context.signal().attributes().get("distanceMeters"));
        Integer durationSeconds = integer(context.signal().attributes().get("durationSeconds"));
        if (distanceMeters == null || durationSeconds == null
                || distanceMeters > context.properties().maxShortTripDistanceMeters()
                || durationSeconds > context.properties().maxShortTripDurationSeconds()) {
            return List.of();
        }
        String pairKey = String.valueOf(context.signal().attributes().getOrDefault("pairKey", context.signal().subjectId()));
        long count = context.windowCounterService().increment(
                "fraud:short-trip:" + pairKey,
                Duration.ofHours(context.properties().shortTripWindowHours())
        );
        if (count < context.properties().shortTripRepeatThreshold()) {
            return List.of();
        }
        return List.of(match(
                FraudRuleCode.SHORT_REPEATED_TRIPS,
                RiskLevel.HIGH,
                new BigDecimal("20.0000"),
                "Repeated short trip pattern detected",
                "A rider-driver pattern is generating repeated short trips beyond the configured threshold.",
                false,
                Map.of(
                        "distanceMeters", distanceMeters,
                        "durationSeconds", durationSeconds,
                        "repeatedTripCount", count
                )
        ));
    }

    private Integer integer(Object value) {
        return value == null ? null : Integer.parseInt(String.valueOf(value));
    }
}
