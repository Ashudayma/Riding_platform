package com.ridingplatform.notification.application;

import com.ridingplatform.notification.domain.NotificationProviderResponse;
import com.ridingplatform.notification.domain.NotificationProviderStatus;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPushNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(LoggingPushNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public NotificationProviderResponse send(NotificationEntity notification) {
        log.info("Sending push notification id={} title={}", notification.getId(), notification.getTitle());
        return new NotificationProviderResponse(NotificationProviderStatus.SENT, "push-log", notification.getId().toString(), "{\"provider\":\"push-log\"}", null);
    }
}
