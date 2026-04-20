package com.ridingplatform.fraud.application;

import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.fraud.domain.FraudSignal;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileEntity;
import java.time.Clock;

public record FraudRuleContext(
        FraudSignal signal,
        FraudRiskProfileEntity profile,
        FraudWindowCounterService windowCounterService,
        ApplicationProperties.Fraud properties,
        Clock clock
) {
}
