package com.ridingplatform.pricing.interfaces;

import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.domain.RideType;
import com.ridingplatform.ride.interfaces.RideStopRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EstimateFareHttpRequest(
        @NotNull RideType rideType,
        @Min(1) @Max(6) short seatCount,
        VehicleType requestedVehicleType,
        @NotBlank String cityCode,
        String zoneCode,
        @Min(-90) @Max(90) double pickupLatitude,
        @Min(-180) @Max(180) double pickupLongitude,
        @NotBlank String pickupAddress,
        @Min(-90) @Max(90) double dropLatitude,
        @Min(-180) @Max(180) double dropLongitude,
        @NotBlank String dropAddress,
        List<RideStopRequest> stops
) {
}
