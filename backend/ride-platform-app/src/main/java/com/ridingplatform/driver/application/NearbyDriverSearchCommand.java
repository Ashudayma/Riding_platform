package com.ridingplatform.driver.application;

import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import java.math.BigDecimal;

public record NearbyDriverSearchCommand(
        double latitude,
        double longitude,
        double radiusMeters,
        int limit,
        AvailabilityStatus availabilityStatus,
        VehicleType vehicleType,
        boolean excludeActiveRide,
        BigDecimal minimumRating,
        boolean excludeRiskBlocked
) {
}
