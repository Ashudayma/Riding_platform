package com.ridingplatform.fraud.interfaces;

import java.time.Instant;
import java.util.UUID;

public record FraudSignalHttpResponse(
        UUID signalId,
        String signalType,
        Instant occurredAt,
        String sourceTopic,
        String triggeredRulesJson
) {
}
