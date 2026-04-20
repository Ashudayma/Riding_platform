package com.ridingplatform.fraud.application;

import com.ridingplatform.fraud.domain.FraudSignal;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileEntity;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NoopRiskScoringModelClient implements RiskScoringModelClient {

    @Override
    public Optional<ModelRiskRecommendation> score(FraudSignal signal, FraudRiskProfileEntity profile) {
        return Optional.empty();
    }
}
