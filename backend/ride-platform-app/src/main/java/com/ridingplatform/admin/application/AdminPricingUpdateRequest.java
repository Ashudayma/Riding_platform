package com.ridingplatform.admin.application;

public record AdminPricingUpdateRequest(
        boolean active,
        String reason
) {
}
