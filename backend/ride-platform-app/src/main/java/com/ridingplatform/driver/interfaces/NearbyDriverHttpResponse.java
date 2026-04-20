package com.ridingplatform.driver.interfaces;

import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NearbyDriverHttpResponse(
        UUID driverProfileId,
        UUID vehicleId,
        VehicleType vehicleType,
        AvailabilityStatus availabilityStatus,
        BigDecimal rating,
        boolean riskBlocked,
        UUID currentRideId,
        double latitude,
        double longitude,
        double distanceMeters,
        Instant lastHeartbeatAt
) {
}
