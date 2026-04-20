package com.ridingplatform.notification.domain;

import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationType;
import java.util.Map;
import java.util.UUID;

public record NotificationDispatchPlan(
        UUID userProfileId,
        UUID rideId,
        NotificationType notificationType,
        NotificationEventCode eventCode,
        NotificationChannel channel,
        String locale,
        Map<String, Object> templateModel
) {
}
