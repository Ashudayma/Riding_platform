package com.ridingplatform.pricing.interfaces;

import com.ridingplatform.pricing.application.EstimateFareCommand;
import com.ridingplatform.pricing.application.FareQuoteResult;
import com.ridingplatform.pricing.application.PricingEngineService;
import com.ridingplatform.ride.application.RideStopCommand;
import com.ridingplatform.ride.interfaces.RideStopRequest;
import com.ridingplatform.security.application.SecurityContextFacade;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import com.ridingplatform.ride.application.RideValidationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pricing")
@Tag(name = "Pricing")
public class PricingController {

    private final PricingEngineService pricingEngineService;
    private final SecurityContextFacade securityContextFacade;
    private final RiderProfileJpaRepository riderProfileJpaRepository;

    public PricingController(
            PricingEngineService pricingEngineService,
            SecurityContextFacade securityContextFacade,
            RiderProfileJpaRepository riderProfileJpaRepository
    ) {
        this.pricingEngineService = pricingEngineService;
        this.securityContextFacade = securityContextFacade;
        this.riderProfileJpaRepository = riderProfileJpaRepository;
    }

    @PostMapping("/estimate")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Estimate fare using configurable pricing rules")
    public ResponseEntity<FareQuoteHttpResponse> estimate(@Valid @RequestBody EstimateFareHttpRequest request) {
        FareQuoteResult result = pricingEngineService.estimate(new EstimateFareCommand(
                resolveAuthenticatedRiderProfileId(),
                request.rideType(),
                request.seatCount(),
                request.requestedVehicleType(),
                request.cityCode(),
                request.zoneCode(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.pickupAddress(),
                request.dropLatitude(),
                request.dropLongitude(),
                request.dropAddress(),
                mapStops(request.stops())
        ));
        return ResponseEntity.ok(toResponse(result));
    }

    @PostMapping("/{fareQuoteId}/finalize")
    @PreAuthorize("hasAnyRole('OPS_ADMIN', 'PLATFORM_ADMIN', 'SUPPORT_AGENT')")
    @Operation(summary = "Finalize fare after ride completion or cancellation")
    public ResponseEntity<FareQuoteHttpResponse> finalizeFare(
            @PathVariable UUID fareQuoteId,
            @Valid @RequestBody FinalizeFareHttpRequest request
    ) {
        // first production version finalizes as standard ride by default unless the quote already includes pool discount.
        FareQuoteResult result = pricingEngineService.finalizeFare(
                new com.ridingplatform.pricing.application.FinalizeFareCommand(
                        fareQuoteId,
                        request.actualDistanceMeters(),
                        request.actualDurationSeconds(),
                        request.waitingDurationSeconds(),
                        request.cancelled(),
                        request.cancelledAfterDistanceMeters()
                ),
                request.rideType()
        );
        return ResponseEntity.ok(toResponse(result));
    }

    private FareQuoteHttpResponse toResponse(FareQuoteResult result) {
        return new FareQuoteHttpResponse(
                result.fareQuoteId(),
                result.rideType(),
                result.cityCode(),
                result.zoneCode(),
                result.pricingVersion(),
                result.baseFare(),
                result.distanceFare(),
                result.durationFare(),
                result.surgeMultiplier(),
                result.surgeAmount(),
                result.bookingFee(),
                result.waitingCharge(),
                result.cancellationCharge(),
                result.taxAmount(),
                result.discountAmount(),
                result.poolingDiscountAmount(),
                result.totalAmount(),
                result.currencyCode(),
                result.distanceMeters(),
                result.durationSeconds(),
                result.expiresAt(),
                result.finalizedAt()
        );
    }

    private UUID resolveAuthenticatedRiderProfileId() {
        UUID userProfileId = securityContextFacade.currentActor()
                .orElseThrow(() -> new RideValidationException("Authenticated actor not found"))
                .userProfileId();
        return riderProfileJpaRepository.findByUserProfileId(userProfileId)
                .orElseThrow(() -> new RideValidationException("Rider profile not found for authenticated user"))
                .getId();
    }

    private List<RideStopCommand> mapStops(List<RideStopRequest> stops) {
        if (stops == null) {
            return List.of();
        }
        return stops.stream()
                .map(stop -> new RideStopCommand(
                        stop.stopType(),
                        stop.latitude(),
                        stop.longitude(),
                        stop.address(),
                        stop.locality()
                ))
                .toList();
    }
}
