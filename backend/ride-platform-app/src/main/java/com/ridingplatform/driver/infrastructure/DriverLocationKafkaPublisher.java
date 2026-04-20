package com.ridingplatform.driver.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.driver.application.DriverLocationEvent;
import com.ridingplatform.driver.application.DriverLocationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DriverLocationKafkaPublisher implements DriverLocationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationKafkaPublisher.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DriverLocationKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DriverLocationEvent event) {
        try {
            kafkaTemplate.send("riding-platform.tracking.location-updated", event.driverProfileId().toString(), objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException exception) {
            log.error("Failed to serialize driver location event for driver={}", event.driverProfileId(), exception);
        }
    }
}
