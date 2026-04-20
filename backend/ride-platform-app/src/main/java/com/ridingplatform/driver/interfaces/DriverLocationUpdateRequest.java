package com.ridingplatform.driver.interfaces;

import com.ridingplatform.tracking.infrastructure.persistence.LocationProviderType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record DriverLocationUpdateRequest(
        @Min(-90) @Max(90) double latitude,
        @Min(-180) @Max(180) double longitude,
        BigDecimal headingDegrees,
        BigDecimal speedKph,
        BigDecimal accuracyMeters,
        @NotNull LocationProviderType locationProvider
) {
}
