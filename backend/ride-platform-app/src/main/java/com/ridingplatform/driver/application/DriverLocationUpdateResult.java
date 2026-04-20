package com.ridingplatform.driver.application;

import java.time.Instant;
import java.util.UUID;

public record DriverLocationUpdateResult(
        UUID driverProfileId,
        double latitude,
        double longitude,
        Instant observedAt
) {
}
