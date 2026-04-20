package com.ridingplatform.fraud.domain;

import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record FraudSignal(
        UUID signalId,
        FraudSignalType signalType,
        FraudSubjectType subjectType,
        UUID subjectId,
        UUID riderProfileId,
        UUID driverProfileId,
        UUID rideRequestId,
        UUID rideId,
        UUID paymentTransactionId,
        String sourceTopic,
        String eventKey,
        Instant occurredAt,
        Map<String, Object> attributes
) {
}
