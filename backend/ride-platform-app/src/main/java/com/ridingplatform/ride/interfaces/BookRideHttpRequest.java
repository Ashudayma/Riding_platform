package com.ridingplatform.ride.interfaces;

import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.domain.RideType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BookRideHttpRequest(
        @NotNull RideType rideType,
        @Min(1) @Max(4) short seatCount,
        VehicleType requestedVehicleType,
        @NotNull UUID paymentMethodId,
        @Min(-90) @Max(90) double pickupLatitude,
        @Min(-180) @Max(180) double pickupLongitude,
        @NotBlank String pickupAddress,
        @Min(-90) @Max(90) double dropLatitude,
        @Min(-180) @Max(180) double dropLongitude,
        @NotBlank String dropAddress,
        @Valid List<RideStopRequest> stops,
        String notes
) {
}
