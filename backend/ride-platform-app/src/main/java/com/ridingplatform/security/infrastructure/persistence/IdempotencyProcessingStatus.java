package com.ridingplatform.security.infrastructure.persistence;

public enum IdempotencyProcessingStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
