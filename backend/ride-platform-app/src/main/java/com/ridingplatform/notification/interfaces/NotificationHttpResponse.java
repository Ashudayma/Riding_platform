package com.ridingplatform.notification.interfaces;

import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryStatus;
import com.ridingplatform.notification.infrastructure.persistence.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationHttpResponse(
        UUID notificationId,
        UUID rideId,
        NotificationType notificationType,
        NotificationEventCode eventCode,
        NotificationChannel channel,
        NotificationDeliveryStatus deliveryStatus,
        String title,
        String body,
        Instant sentAt,
        Instant deliveredAt,
        Instant readAt,
        String failureReason
) {
}
