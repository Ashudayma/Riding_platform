package com.ridingplatform.admin.application;

import java.math.BigDecimal;

public record AdminSharedRidePerformanceView(
        long totalGroups,
        long openGroups,
        long completedGroups,
        double averageSeatUtilization,
        BigDecimal totalPoolingSavings
) {
}
