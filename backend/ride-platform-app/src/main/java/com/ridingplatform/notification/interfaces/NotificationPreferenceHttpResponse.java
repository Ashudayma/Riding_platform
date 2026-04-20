package com.ridingplatform.notification.interfaces;

import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import java.util.UUID;

public record NotificationPreferenceHttpResponse(
        UUID preferenceId,
        NotificationEventCode eventCode,
        NotificationChannel channel,
        boolean enabled
) {
}
