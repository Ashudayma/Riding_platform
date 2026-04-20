package com.ridingplatform.pricing.application;

import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.domain.RideType;

public record PricingRequestContext(
        String cityCode,
        String zoneCode,
        RideType rideType,
        VehicleType vehicleType
) {
}
