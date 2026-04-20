package com.ridingplatform.admin.application;

public record AdminOperationalMetricsView(
        long ridesInProgress,
        long ridesSearchingDriver,
        long availableDrivers,
        long blockedDrivers,
        long blockedRiders,
        long openFraudAlerts,
        long openSharedRideGroups
) {
}
