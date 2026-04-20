package com.ridingplatform.fraud.application;

import com.ridingplatform.fraud.domain.FraudRuleMatch;
import com.ridingplatform.fraud.domain.FraudSignalType;
import java.util.List;
import java.util.Set;

public interface FraudRule {

    Set<FraudSignalType> supports();

    List<FraudRuleMatch> evaluate(FraudRuleContext context);
}
