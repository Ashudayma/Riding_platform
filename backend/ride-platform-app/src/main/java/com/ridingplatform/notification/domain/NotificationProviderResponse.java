package com.ridingplatform.notification.domain;

public record NotificationProviderResponse(
        NotificationProviderStatus status,
        String providerKey,
        String providerMessageRef,
        String responsePayloadJson,
        String failureReason
) {
}
