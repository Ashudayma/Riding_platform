package com.ridingplatform.driver.application;

import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.OnlineStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverAvailabilityResult(
        UUID driverProfileId,
        AvailabilityStatus availabilityStatus,
        OnlineStatus onlineStatus,
        short availableSeatCount,
        UUID currentRideId,
        double latitude,
        double longitude,
        BigDecimal accuracyMeters,
        Instant lastHeartbeatAt
) {
}
