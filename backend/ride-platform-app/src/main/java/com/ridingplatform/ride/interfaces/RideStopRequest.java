package com.ridingplatform.ride.interfaces;

import com.ridingplatform.ride.application.StopTypeCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RideStopRequest(
        @NotNull StopTypeCommand stopType,
        @Min(-90) @Max(90) double latitude,
        @Min(-180) @Max(180) double longitude,
        @NotBlank String address,
        String locality
) {
}
