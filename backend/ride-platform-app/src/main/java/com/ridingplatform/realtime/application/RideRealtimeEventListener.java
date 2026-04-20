package com.ridingplatform.realtime.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.driver.application.DriverLocationEvent;
import com.ridingplatform.ride.application.DriverAssignmentEventPayload;
import com.ridingplatform.ride.application.RideEventType;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RideRealtimeEventListener {

    private final ObjectMapper objectMapper;
    private final RideRealtimeGateway rideRealtimeGateway;

    public RideRealtimeEventListener(ObjectMapper objectMapper, RideRealtimeGateway rideRealtimeGateway) {
        this.objectMapper = objectMapper;
        this.rideRealtimeGateway = rideRealtimeGateway;
    }

    @KafkaListener(topics = {
            "riding-platform.ride.requested",
            "riding-platform.ride.cancelled"
    }, groupId = "realtime-ride-lifecycle")
    public void onRideLifecycleEvent(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        RideEventType eventType = RideEventType.valueOf(root.get("eventType").asText());
        UUID rideRequestId = UUID.fromString(root.get("aggregateId").asText());
        String status = switch (eventType) {
            case BOOKING_REQUESTED -> "SEARCHING_DRIVER";
            case BOOKING_CANCELLED -> "CANCELLED";
            default -> eventType.name();
        };
        String text = switch (eventType) {
            case BOOKING_REQUESTED -> "Ride request created";
            case BOOKING_CANCELLED -> "Ride cancelled";
            default -> eventType.name();
        };
        rideRealtimeGateway.publishRideEvent(eventType, rideRequestId, status, text);
    }

    @KafkaListener(topics = {
            "riding-platform.dispatch.driver-assigned",
            "riding-platform.dispatch.assignment-failed",
            "riding-platform.dispatch.assignment-timed-out"
    }, groupId = "realtime-dispatch")
    public void onAssignmentEvent(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        DriverAssignmentEventPayload payload = objectMapper.treeToValue(root.get("payload"), DriverAssignmentEventPayload.class);
        rideRealtimeGateway.publishAssignmentEvent(payload);
    }

    @KafkaListener(topics = "riding-platform.tracking.location-updated", groupId = "realtime-driver-location")
    public void onDriverLocation(String payload) throws Exception {
        DriverLocationEvent locationEvent = objectMapper.readValue(payload, DriverLocationEvent.class);
        if (locationEvent.currentRideId() != null) {
            rideRealtimeGateway.publishDriverLocation(locationEvent);
        }
    }
}
