package com.ridingplatform.driver.interfaces;

import java.time.Instant;
import java.util.UUID;

public record DriverLocationHttpResponse(
        UUID driverProfileId,
        double latitude,
        double longitude,
        Instant observedAt
) {
}
