package com.ridingplatform.notification.domain;

import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import java.util.UUID;

public record NotificationPreferenceView(
        UUID preferenceId,
        NotificationEventCode eventCode,
        NotificationChannel channel,
        boolean enabled
) {
}
