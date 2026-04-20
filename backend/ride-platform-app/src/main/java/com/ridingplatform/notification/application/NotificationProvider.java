package com.ridingplatform.notification.application;

import com.ridingplatform.notification.domain.NotificationProviderResponse;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationEntity;

public interface NotificationProvider {

    NotificationChannel channel();

    NotificationProviderResponse send(NotificationEntity notification);
}
