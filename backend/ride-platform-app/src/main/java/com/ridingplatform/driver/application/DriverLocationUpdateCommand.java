package com.ridingplatform.driver.application;

import com.ridingplatform.tracking.infrastructure.persistence.LocationProviderType;
import java.math.BigDecimal;

public record DriverLocationUpdateCommand(
        double latitude,
        double longitude,
        BigDecimal headingDegrees,
        BigDecimal speedKph,
        BigDecimal accuracyMeters,
        LocationProviderType locationProvider
) {
}
