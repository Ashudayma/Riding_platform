package com.ridingplatform.driver.application;

import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.OnlineStatus;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DriverDispatchCandidate(
        UUID driverProfileId,
        UUID vehicleId,
        VehicleType vehicleType,
        short seatCapacity,
        AvailabilityStatus availabilityStatus,
        OnlineStatus onlineStatus,
        UUID currentRideId,
        BigDecimal averageRating,
        BigDecimal acceptanceRate,
        BigDecimal cancellationRate,
        BigDecimal riskScore,
        boolean riskBlocked,
        Instant availableSince,
        Instant lastHeartbeatAt
) {
}
