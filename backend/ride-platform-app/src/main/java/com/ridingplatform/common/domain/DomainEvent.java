package com.ridingplatform.common.domain;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    Instant occurredAt();

    String eventType();
}
