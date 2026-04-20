package com.ridingplatform.driver.interfaces;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DriverAvailabilityRequest(
        boolean online,
        @Min(0) @Max(12) short availableSeatCount,
        @NotBlank String sessionId,
        String appVersion,
        String devicePlatform
) {
}
