package com.ridingplatform.ride.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.ride.application.RideEventPublisher;
import com.ridingplatform.ride.application.RideEventType;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RideKafkaEventPublisher implements RideEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RideKafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public RideKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(RideEventType eventType, UUID aggregateId, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(Map.of(
                    "eventType", eventType.name(),
                    "aggregateId", aggregateId,
                    "payload", payload
            ));
            kafkaTemplate.send(topic(eventType), aggregateId.toString(), message);
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize ride event type={} aggregateId={}", eventType, aggregateId, exception);
        }
    }

        private String topic(RideEventType eventType) {
        return switch (eventType) {
            case FARE_ESTIMATED -> "riding-platform.pricing.quote-generated";
            case BOOKING_REQUESTED -> "riding-platform.ride.requested";
            case BOOKING_CANCELLED -> "riding-platform.ride.cancelled";
            case DRIVER_ASSIGNED -> "riding-platform.dispatch.driver-assigned";
            case DRIVER_ASSIGNMENT_FAILED -> "riding-platform.dispatch.assignment-failed";
            case DRIVER_ASSIGNMENT_TIMED_OUT -> "riding-platform.dispatch.assignment-timed-out";
        };
    }
}
