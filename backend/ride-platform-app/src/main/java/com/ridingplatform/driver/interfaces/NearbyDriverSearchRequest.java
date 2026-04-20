package com.ridingplatform.driver.interfaces;

import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record NearbyDriverSearchRequest(
        @Min(-90) @Max(90) double latitude,
        @Min(-180) @Max(180) double longitude,
        @Min(100) @Max(20000) double radiusMeters,
        @Min(1) @Max(200) int limit,
        AvailabilityStatus availabilityStatus,
        VehicleType vehicleType,
        boolean excludeActiveRide,
        BigDecimal minimumRating,
        boolean excludeRiskBlocked
) {
}
