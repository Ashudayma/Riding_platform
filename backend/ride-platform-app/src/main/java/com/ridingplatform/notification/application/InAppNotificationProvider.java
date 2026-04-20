package com.ridingplatform.notification.application;

import com.ridingplatform.notification.domain.NotificationProviderResponse;
import com.ridingplatform.notification.domain.NotificationProviderStatus;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationEntity;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationProvider implements NotificationProvider {

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public NotificationProviderResponse send(NotificationEntity notification) {
        return new NotificationProviderResponse(
                NotificationProviderStatus.DELIVERED,
                "in-app",
                notification.getId().toString(),
                "{\"channel\":\"IN_APP\"}",
                null
        );
    }
}
