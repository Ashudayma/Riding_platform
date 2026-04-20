package com.ridingplatform.notification.domain;

import java.util.List;

public record NotificationEventEnvelope(
        String eventKey,
        List<NotificationDispatchPlan> dispatchPlans
) {
}
