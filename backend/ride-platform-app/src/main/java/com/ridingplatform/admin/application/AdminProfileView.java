package com.ridingplatform.admin.application;

import java.math.BigDecimal;
import java.util.UUID;

public record AdminProfileView(
        UUID profileId,
        UUID userProfileId,
        String code,
        String status,
        BigDecimal averageRating,
        BigDecimal riskScore,
        boolean blocked,
        boolean fraudHold,
        boolean fraudBlocked,
        String displayName,
        String email
) {
}
