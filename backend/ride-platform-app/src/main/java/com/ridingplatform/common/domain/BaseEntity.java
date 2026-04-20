package com.ridingplatform.common.domain;

import java.time.Instant;
import java.util.UUID;

public abstract class BaseEntity {

    private final UUID id;
    private final Instant createdAt;
    private Instant updatedAt;

    protected BaseEntity(UUID id, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    protected void touch(Instant timestamp) {
        this.updatedAt = timestamp;
    }
}
