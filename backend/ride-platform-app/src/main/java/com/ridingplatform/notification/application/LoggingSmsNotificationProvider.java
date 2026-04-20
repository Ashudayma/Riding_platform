package com.ridingplatform.notification.application;

import com.ridingplatform.notification.domain.NotificationProviderResponse;
import com.ridingplatform.notification.domain.NotificationProviderStatus;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingSmsNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public NotificationProviderResponse send(NotificationEntity notification) {
        log.info("Sending sms notification id={} body={}", notification.getId(), notification.getBody());
        return new NotificationProviderResponse(NotificationProviderStatus.SENT, "sms-log", notification.getId().toString(), "{\"provider\":\"sms-log\"}", null);
    }
}
