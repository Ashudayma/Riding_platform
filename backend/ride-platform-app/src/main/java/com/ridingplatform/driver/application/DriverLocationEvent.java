package com.ridingplatform.driver.application;

import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.tracking.infrastructure.persistence.LocationProviderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverLocationEvent(
        UUID eventId,
        UUID driverProfileId,
        UUID currentRideId,
        VehicleType vehicleType,
        double latitude,
        double longitude,
        BigDecimal headingDegrees,
        BigDecimal speedKph,
        BigDecimal accuracyMeters,
        LocationProviderType locationProvider,
        Instant observedAt
) {
}
