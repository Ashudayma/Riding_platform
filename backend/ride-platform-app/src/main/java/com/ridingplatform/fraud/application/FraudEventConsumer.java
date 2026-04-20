package com.ridingplatform.fraud.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.fraud.domain.FraudSignal;
import com.ridingplatform.fraud.domain.FraudSignalType;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FraudEventConsumer {

    private final ObjectMapper objectMapper;
    private final IdGenerator idGenerator;
    private final FraudRiskService fraudRiskService;
    private final Clock clock;

    public FraudEventConsumer(ObjectMapper objectMapper, IdGenerator idGenerator, FraudRiskService fraudRiskService, Clock clock) {
        this.objectMapper = objectMapper;
        this.idGenerator = idGenerator;
        this.fraudRiskService = fraudRiskService;
        this.clock = clock;
    }

    @KafkaListener(topics = "riding-platform.ride.cancelled", groupId = "fraud-risk-engine")
    public void onRideCancelled(String rawMessage) throws Exception {
        JsonNode root = objectMapper.readTree(rawMessage).path("payload");
        UUID riderProfileId = uuid(root, "riderProfileId");
        if (riderProfileId == null) {
            return;
        }
        fraudRiskService.processSignal(new FraudSignal(
                idGenerator.nextId(),
                FraudSignalType.RIDE_CANCELLED,
                FraudSubjectType.RIDER,
                riderProfileId,
                riderProfileId,
                null,
                uuid(root, "rideRequestId"),
                uuid(root, "rideId"),
                null,
                "riding-platform.ride.cancelled",
                riderProfileId.toString(),
                Instant.now(clock),
                Map.of("status", root.path("status").asText(""))
        ));
    }

    @KafkaListener(topics = "riding-platform.tracking.location-updated", groupId = "fraud-location-engine")
    public void onDriverLocationUpdated(String rawMessage) throws Exception {
        JsonNode root = objectMapper.readTree(rawMessage);
        UUID driverProfileId = uuid(root, "driverProfileId");
        if (driverProfileId == null) {
            return;
        }
        fraudRiskService.processSignal(new FraudSignal(
                idGenerator.nextId(),
                FraudSignalType.DRIVER_LOCATION_UPDATE,
                FraudSubjectType.DRIVER,
                driverProfileId,
                null,
                driverProfileId,
                null,
                uuid(root, "currentRideId"),
                null,
                "riding-platform.tracking.location-updated",
                driverProfileId.toString(),
                Instant.now(clock),
                Map.of(
                        "speedKph", root.path("speedKph").decimalValue(),
                        "accuracyMeters", root.path("accuracyMeters").decimalValue(),
                        "locationProvider", root.path("locationProvider").asText(null)
                )
        ));
    }

    @KafkaListener(topics = "riding-platform.payment.transaction-updated", groupId = "fraud-payment-engine")
    public void onPaymentTransactionUpdated(String rawMessage) throws Exception {
        JsonNode root = objectMapper.readTree(rawMessage);
        if (!"FAILED".equalsIgnoreCase(root.path("transactionStatus").asText())) {
            return;
        }
        UUID riderProfileId = uuid(root, "riderProfileId");
        if (riderProfileId == null) {
            return;
        }
        fraudRiskService.processSignal(new FraudSignal(
                idGenerator.nextId(),
                FraudSignalType.PAYMENT_FAILED,
                FraudSubjectType.RIDER,
                riderProfileId,
                riderProfileId,
                null,
                uuid(root, "rideRequestId"),
                uuid(root, "rideId"),
                uuid(root, "paymentTransactionId"),
                "riding-platform.payment.transaction-updated",
                riderProfileId.toString(),
                Instant.now(clock),
                Map.of(
                        "provider", root.path("paymentProvider").asText(""),
                        "failureReason", root.path("failureReason").asText("")
                )
        ));
    }

    private UUID uuid(JsonNode root, String fieldName) {
        String value = root.path(fieldName).asText(null);
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return UUID.fromString(value);
    }
}
