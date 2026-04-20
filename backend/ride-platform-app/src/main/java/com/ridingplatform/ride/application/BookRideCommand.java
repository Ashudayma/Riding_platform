package com.ridingplatform.ride.application;

import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.domain.RideType;
import java.util.List;
import java.util.UUID;

public record BookRideCommand(
        UUID riderProfileId,
        RideType rideType,
        short seatCount,
        VehicleType requestedVehicleType,
        UUID paymentMethodId,
        double pickupLatitude,
        double pickupLongitude,
        String pickupAddress,
        double dropLatitude,
        double dropLongitude,
        String dropAddress,
        List<RideStopCommand> stops,
        String notes
) {
}
