package com.ridingplatform.notification.domain;

public record NotificationTemplateContent(
        String templateKey,
        String title,
        String body
) {
}
