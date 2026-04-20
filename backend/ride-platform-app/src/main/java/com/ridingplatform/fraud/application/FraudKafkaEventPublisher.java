package com.ridingplatform.fraud.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class FraudKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(FraudKafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FraudKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishProfileUpdated(UUID subjectId, Object payload) {
        publish("riding-platform.fraud.profile-updated", subjectId, payload);
    }

    public void publishFlagRaised(UUID flagId, Object payload) {
        publish("riding-platform.fraud.flag-raised", flagId, payload);
    }

    private void publish(String topic, UUID key, Object payload) {
        try {
            kafkaTemplate.send(topic, key.toString(), objectMapper.writeValueAsString(Map.of("payload", payload)));
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize fraud event topic={} key={}", topic, key, exception);
        }
    }
}
