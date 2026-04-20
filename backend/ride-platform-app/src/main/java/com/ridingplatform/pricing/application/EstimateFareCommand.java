package com.ridingplatform.pricing.application;

import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.application.RideStopCommand;
import com.ridingplatform.ride.domain.RideType;
import java.util.List;
import java.util.UUID;

public record EstimateFareCommand(
        UUID riderProfileId,
        RideType rideType,
        short seatCount,
        VehicleType requestedVehicleType,
        String cityCode,
        String zoneCode,
        double pickupLatitude,
        double pickupLongitude,
        String pickupAddress,
        double dropLatitude,
        double dropLongitude,
        String dropAddress,
        List<RideStopCommand> stops
) {
}
