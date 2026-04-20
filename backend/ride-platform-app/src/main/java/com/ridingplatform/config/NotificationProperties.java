package com.ridingplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform.notification")
public record NotificationProperties(
        int maxRetryAttempts,
        int initialRetryDelaySeconds,
        int retryScanBatchSize,
        String defaultLocale
) {
}
