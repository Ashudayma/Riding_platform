package com.ridingplatform.ride.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RideDispatchEventListener {

    private final ObjectMapper objectMapper;
    private final DriverAssignmentService driverAssignmentService;

    public RideDispatchEventListener(ObjectMapper objectMapper, DriverAssignmentService driverAssignmentService) {
        this.objectMapper = objectMapper;
        this.driverAssignmentService = driverAssignmentService;
    }

    @KafkaListener(topics = "riding-platform.ride.requested", groupId = "driver-assignment-engine")
    public void onRideRequested(String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode aggregateId = root.get("aggregateId");
        if (aggregateId == null || aggregateId.isNull()) {
            return;
        }
        driverAssignmentService.assignBestDriver(new DriverAssignmentRequest(
                UUID.fromString(aggregateId.asText()),
                AssignmentTrigger.BOOKING_REQUESTED
        ));
    }
}
