package com.ridingplatform.realtime.application;

import java.time.Instant;

public record SessionStatePayload(
        String state,
        String sessionId,
        Instant occurredAt
) {
}
