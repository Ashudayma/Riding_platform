package com.ridingplatform.admin.application;

public record AdminDispatchStatsView(
        long totalAttempts,
        long acceptedAttempts,
        long rejectedAttempts,
        long timedOutAttempts,
        long failedAttempts
) {
}
