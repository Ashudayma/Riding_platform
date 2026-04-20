package com.ridingplatform.notification.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import com.ridingplatform.notification.domain.NotificationDispatchPlan;
import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationType;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationApplicationService notificationApplicationService;
    private final RideJpaRepository rideJpaRepository;
    private final RiderProfileJpaRepository riderProfileJpaRepository;
    private final DriverProfileJpaRepository driverProfileJpaRepository;

    public NotificationEventConsumer(
            ObjectMapper objectMapper,
            NotificationApplicationService notificationApplicationService,
            RideJpaRepository rideJpaRepository,
            RiderProfileJpaRepository riderProfileJpaRepository,
            DriverProfileJpaRepository driverProfileJpaRepository
    ) {
        this.objectMapper = objectMapper;
        this.notificationApplicationService = notificationApplicationService;
        this.rideJpaRepository = rideJpaRepository;
        this.riderProfileJpaRepository = riderProfileJpaRepository;
        this.driverProfileJpaRepository = driverProfileJpaRepository;
    }

    @KafkaListener(topics = "riding-platform.ride.requested", groupId = "notification-ride-booked")
    public void onRideBooked(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        JsonNode payload = root.path("payload");
        UUID rideRequestId = uuid(root, "aggregateId");
        UUID rideId = uuid(payload, "rideId");
        UUID riderProfileId = uuid(payload, "riderProfileId");
        String rideCode = rideJpaRepository.findNotificationAudienceByRideRequestId(rideRequestId)
                .map(RideNotificationAudience::rideCode)
                .orElse("RIDE");
        UUID userProfileId = riderProfileJpaRepository.findUserProfileIdByRiderProfileId(riderProfileId).orElse(null);
        if (userProfileId == null) {
            return;
        }
        notificationApplicationService.dispatch(List.of(
                plan(userProfileId, rideId, NotificationType.RIDE_UPDATE, NotificationEventCode.RIDE_BOOKED, NotificationChannel.IN_APP, Map.of("rideCode", rideCode)),
                plan(userProfileId, rideId, NotificationType.RIDE_UPDATE, NotificationEventCode.RIDE_BOOKED, NotificationChannel.PUSH, Map.of("rideCode", rideCode))
        ));
    }

    @KafkaListener(topics = "riding-platform.dispatch.driver-assigned", groupId = "notification-driver-assigned")
    public void onDriverAssigned(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        JsonNode payload = root.path("payload");
        UUID rideRequestId = uuid(payload, "rideRequestId");
        rideJpaRepository.findNotificationAudienceByRideRequestId(rideRequestId).ifPresent(audience -> {
            List<NotificationDispatchPlan> plans = new ArrayList<>();
            if (audience.riderUserProfileId() != null) {
                plans.add(plan(audience.riderUserProfileId(), audience.rideId(), NotificationType.RIDE_UPDATE, NotificationEventCode.DRIVER_ASSIGNED_RIDER, NotificationChannel.IN_APP,
                        Map.of("rideCode", audience.rideCode(), "driverName", audience.driverDisplayName())));
                plans.add(plan(audience.riderUserProfileId(), audience.rideId(), NotificationType.RIDE_UPDATE, NotificationEventCode.DRIVER_ASSIGNED_RIDER, NotificationChannel.PUSH,
                        Map.of("rideCode", audience.rideCode(), "driverName", audience.driverDisplayName())));
            }
            if (audience.driverUserProfileId() != null) {
                plans.add(plan(audience.driverUserProfileId(), audience.rideId(), NotificationType.RIDE_UPDATE, NotificationEventCode.DRIVER_ASSIGNED_DRIVER, NotificationChannel.IN_APP,
                        Map.of("rideCode", audience.rideCode(), "driverName", audience.driverDisplayName())));
                plans.add(plan(audience.driverUserProfileId(), audience.rideId(), NotificationType.RIDE_UPDATE, NotificationEventCode.DRIVER_ASSIGNED_DRIVER, NotificationChannel.PUSH,
                        Map.of("rideCode", audience.rideCode(), "driverName", audience.driverDisplayName())));
            }
            notificationApplicationService.dispatch(plans);
        });
    }

    @KafkaListener(topics = {
            "riding-platform.ride.cancelled",
            "riding-platform.ride.driver-arrived",
            "riding-platform.ride.started",
            "riding-platform.ride.completed"
    }, groupId = "notification-ride-lifecycle")
    public void onRideLifecycle(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        String eventType = root.path("eventType").asText("");
        JsonNode payload = root.path("payload");
        UUID rideRequestId = uuid(root, "aggregateId");
        NotificationEventCode eventCode = switch (eventType) {
            case "BOOKING_CANCELLED" -> NotificationEventCode.RIDE_CANCELLED;
            case "DRIVER_ARRIVED" -> NotificationEventCode.DRIVER_ARRIVED;
            case "RIDE_STARTED" -> NotificationEventCode.RIDE_STARTED;
            case "RIDE_COMPLETED" -> NotificationEventCode.RIDE_COMPLETED;
            default -> null;
        };
        if (eventCode == null) {
            return;
        }
        rideJpaRepository.findNotificationAudienceByRideRequestId(rideRequestId).ifPresent(audience -> {
            List<NotificationDispatchPlan> plans = new ArrayList<>();
            Map<String, Object> model = Map.of("rideCode", audience.rideCode(), "driverName", audience.driverDisplayName());
            if (audience.riderUserProfileId() != null) {
                plans.add(plan(audience.riderUserProfileId(), audience.rideId(), NotificationType.RIDE_UPDATE, eventCode, NotificationChannel.IN_APP, model));
                plans.add(plan(audience.riderUserProfileId(), audience.rideId(), NotificationType.RIDE_UPDATE, eventCode, NotificationChannel.PUSH, model));
            }
            if (audience.driverUserProfileId() != null && eventCode == NotificationEventCode.RIDE_CANCELLED) {
                plans.add(plan(audience.driverUserProfileId(), audience.rideId(), NotificationType.RIDE_UPDATE, eventCode, NotificationChannel.IN_APP, model));
            }
            notificationApplicationService.dispatch(plans);
        });
    }

    @KafkaListener(topics = "riding-platform.fraud.profile-updated", groupId = "notification-fraud")
    public void onFraudProfileUpdated(String message) throws Exception {
        JsonNode payload = objectMapper.readTree(message).path("payload");
        if (!payload.path("blocked").asBoolean(false)) {
            return;
        }
        FraudSubjectType subjectType = FraudSubjectType.valueOf(payload.path("subjectType").asText());
        UUID subjectId = UUID.fromString(payload.path("subjectId").asText());
        UUID userProfileId = switch (subjectType) {
            case RIDER -> riderProfileJpaRepository.findUserProfileIdByRiderProfileId(subjectId).orElse(null);
            case DRIVER -> driverProfileJpaRepository.findUserProfileIdByDriverProfileId(subjectId).orElse(null);
            default -> null;
        };
        if (userProfileId == null) {
            return;
        }
        notificationApplicationService.dispatch(List.of(
                plan(userProfileId, null, NotificationType.SECURITY, NotificationEventCode.ACCOUNT_BLOCKED, NotificationChannel.IN_APP, Map.of("subjectType", subjectType.name())),
                plan(userProfileId, null, NotificationType.SECURITY, NotificationEventCode.ACCOUNT_BLOCKED, NotificationChannel.EMAIL, Map.of("subjectType", subjectType.name())),
                plan(userProfileId, null, NotificationType.SECURITY, NotificationEventCode.ACCOUNT_BLOCKED, NotificationChannel.SMS, Map.of("subjectType", subjectType.name()))
        ));
    }

    @KafkaListener(topics = "riding-platform.payment.transaction-updated", groupId = "notification-payment")
    public void onPaymentUpdated(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);
        String status = root.path("transactionStatus").asText("");
        UUID rideId = uuid(root, "rideId");
        UUID userProfileId = uuid(root, "userProfileId");
        String rideCode = rideId == null ? "RIDE" : rideJpaRepository.findNotificationAudienceByRideId(rideId).map(RideNotificationAudience::rideCode).orElse("RIDE");
        NotificationEventCode eventCode = switch (status) {
            case "FAILED" -> NotificationEventCode.PAYMENT_FAILED;
            case "CAPTURED" -> NotificationEventCode.PAYMENT_CAPTURED;
            default -> null;
        };
        if (eventCode == null || userProfileId == null) {
            return;
        }
        notificationApplicationService.dispatch(List.of(
                plan(userProfileId, rideId, NotificationType.PAYMENT, eventCode, NotificationChannel.IN_APP, Map.of("rideCode", rideCode)),
                plan(userProfileId, rideId, NotificationType.PAYMENT, eventCode, NotificationChannel.EMAIL, Map.of("rideCode", rideCode))
        ));
    }

    private NotificationDispatchPlan plan(
            UUID userProfileId,
            UUID rideId,
            NotificationType notificationType,
            NotificationEventCode eventCode,
            NotificationChannel channel,
            Map<String, Object> model
    ) {
        return new NotificationDispatchPlan(userProfileId, rideId, notificationType, eventCode, channel, "en", model);
    }

    private UUID uuid(JsonNode node, String field) {
        String text = node.path(field).asText(null);
        if (text == null || text.isBlank() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return UUID.fromString(text);
    }
}
