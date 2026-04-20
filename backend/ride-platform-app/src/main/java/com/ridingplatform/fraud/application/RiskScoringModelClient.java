package com.ridingplatform.fraud.application;

import com.ridingplatform.fraud.domain.FraudSignal;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileEntity;
import java.util.Optional;

public interface RiskScoringModelClient {

    Optional<ModelRiskRecommendation> score(FraudSignal signal, FraudRiskProfileEntity profile);
}
