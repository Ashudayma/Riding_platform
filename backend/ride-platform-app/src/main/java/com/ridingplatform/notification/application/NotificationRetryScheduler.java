package com.ridingplatform.notification.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationRetryScheduler {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationRetryScheduler(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @Scheduled(fixedDelay = 10000)
    public void retryPendingFailures() {
        notificationApplicationService.retryDueNotifications();
    }
}
