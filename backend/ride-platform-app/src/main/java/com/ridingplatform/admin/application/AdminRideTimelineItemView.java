package com.ridingplatform.admin.application;

import java.time.Instant;
import java.util.UUID;

public record AdminRideTimelineItemView(
        UUID historyId,
        String previousStatus,
        String currentStatus,
        String sourceType,
        String actorType,
        String note,
        Instant changedAt
) {
}
