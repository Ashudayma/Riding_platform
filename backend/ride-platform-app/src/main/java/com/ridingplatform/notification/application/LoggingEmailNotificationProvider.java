package com.ridingplatform.notification.application;

import com.ridingplatform.notification.domain.NotificationProviderResponse;
import com.ridingplatform.notification.domain.NotificationProviderStatus;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEmailNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailNotificationProvider.class);

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationProviderResponse send(NotificationEntity notification) {
        log.info("Sending email notification id={} title={}", notification.getId(), notification.getTitle());
        return new NotificationProviderResponse(NotificationProviderStatus.SENT, "email-log", notification.getId().toString(), "{\"provider\":\"email-log\"}", null);
    }
}
