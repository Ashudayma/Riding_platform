package com.ridingplatform.realtime.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverLocationRealtimePayload(
        UUID rideId,
        UUID driverProfileId,
        double latitude,
        double longitude,
        BigDecimal headingDegrees,
        BigDecimal speedKph,
        BigDecimal accuracyMeters,
        Instant observedAt
) {
}
