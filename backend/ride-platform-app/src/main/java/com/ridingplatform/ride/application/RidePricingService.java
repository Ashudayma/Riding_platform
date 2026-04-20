package com.ridingplatform.ride.application;

import com.ridingplatform.pricing.application.EstimateFareCommand;
import com.ridingplatform.pricing.application.FareQuoteResult;
import com.ridingplatform.pricing.application.FinalizeFareCommand;
import com.ridingplatform.pricing.application.PricingEngineService;
import com.ridingplatform.ride.domain.RideType;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RidePricingService {

    private final PricingEngineService pricingEngineService;

    public RidePricingService(PricingEngineService pricingEngineService) {
        this.pricingEngineService = pricingEngineService;
    }

    public FareEstimateResult estimate(FareEstimateCommand command) {
        validateStops(command.stops(), command.rideType());
        FareQuoteResult result = pricingEngineService.estimate(new EstimateFareCommand(
                command.riderProfileId(),
                command.rideType(),
                command.seatCount(),
                command.requestedVehicleType(),
                "DELHI_NCR",
                resolveZoneCode(command.pickupLatitude(), command.pickupLongitude()),
                command.pickupLatitude(),
                command.pickupLongitude(),
                command.pickupAddress(),
                command.dropLatitude(),
                command.dropLongitude(),
                command.dropAddress(),
                command.stops()
        ));
        return new FareEstimateResult(
                result.fareQuoteId(),
                result.rideType(),
                result.baseFare(),
                result.distanceFare(),
                result.durationFare(),
                result.surgeMultiplier(),
                result.bookingFee(),
                result.taxAmount(),
                result.discountAmount(),
                result.poolingDiscountAmount(),
                result.totalAmount(),
                result.currencyCode(),
                result.distanceMeters(),
                result.durationSeconds(),
                result.expiresAt()
        );
    }

    public FareQuoteResult finalizeFare(UUID fareQuoteId, RideType rideType, int distanceMeters, int durationSeconds, int waitingSeconds, boolean cancelled, int cancelledAfterDistanceMeters) {
        return pricingEngineService.finalizeFare(
                new FinalizeFareCommand(fareQuoteId, distanceMeters, durationSeconds, waitingSeconds, cancelled, cancelledAfterDistanceMeters),
                rideType
        );
    }

    private void validateStops(List<RideStopCommand> stops, RideType rideType) {
        if (stops == null) {
            return;
        }
        long waypoints = stops.stream().filter(stop -> stop.stopType() == StopTypeCommand.WAYPOINT).count();
        if (waypoints > 5) {
            throw new RideValidationException("At most 5 optional waypoints are supported");
        }
        if (rideType == RideType.SHARED && waypoints > 2) {
            throw new RideValidationException("Shared rides support at most 2 intermediate waypoints");
        }
    }

    private String resolveZoneCode(double latitude, double longitude) {
        if (latitude >= 28.55d && latitude <= 28.75d && longitude >= 77.05d && longitude <= 77.30d) {
            return "CENTRAL";
        }
        return "DEFAULT";
    }
}
