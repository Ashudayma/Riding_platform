package com.ridingplatform.notification.interfaces;

import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceHttpRequest(
        @NotNull NotificationEventCode eventCode,
        @NotNull NotificationChannel channel,
        boolean enabled
) {
}
