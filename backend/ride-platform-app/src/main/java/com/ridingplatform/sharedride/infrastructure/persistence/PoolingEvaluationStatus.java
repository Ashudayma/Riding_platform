package com.ridingplatform.sharedride.infrastructure.persistence;

public enum PoolingEvaluationStatus {
    PENDING,
    COMPATIBLE,
    INCOMPATIBLE,
    ACCEPTED,
    REJECTED,
    EXPIRED
}
