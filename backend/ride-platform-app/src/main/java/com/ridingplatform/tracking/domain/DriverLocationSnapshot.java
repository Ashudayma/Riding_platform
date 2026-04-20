package com.ridingplatform.tracking.domain;

import java.time.Instant;
import java.util.UUID;

public record DriverLocationSnapshot(
        UUID driverId,
        double latitude,
        double longitude,
        double heading,
        double speedKph,
        Instant observedAt
) {
}
