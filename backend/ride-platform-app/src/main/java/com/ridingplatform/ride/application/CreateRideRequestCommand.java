package com.ridingplatform.ride.application;

import com.ridingplatform.ride.domain.RideType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateRideRequestCommand(
        @NotNull UUID riderId,
        @NotNull RideType rideType,
        @Min(-90) @Max(90) double originLatitude,
        @Min(-180) @Max(180) double originLongitude,
        @NotBlank String originAddress,
        @Min(-90) @Max(90) double destinationLatitude,
        @Min(-180) @Max(180) double destinationLongitude,
        @NotBlank String destinationAddress
) {
}
