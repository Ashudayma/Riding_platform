package com.ridingplatform.ride.interfaces;

import com.ridingplatform.ride.application.BookRideCommand;
import com.ridingplatform.ride.application.CancelRideCommand;
import com.ridingplatform.ride.application.FareEstimateCommand;
import com.ridingplatform.ride.application.RideApplicationService;
import com.ridingplatform.ride.application.RideHistoryItemResult;
import com.ridingplatform.ride.application.RideStopCommand;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import com.ridingplatform.security.application.SecurityContextFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rides")
@Tag(name = "Ride Booking")
public class RideController {

    private final RideApplicationService rideApplicationService;
    private final SecurityContextFacade securityContextFacade;
    private final RiderProfileJpaRepository riderProfileJpaRepository;

    public RideController(
            RideApplicationService rideApplicationService,
            SecurityContextFacade securityContextFacade,
            RiderProfileJpaRepository riderProfileJpaRepository
    ) {
        this.rideApplicationService = rideApplicationService;
        this.securityContextFacade = securityContextFacade;
        this.riderProfileJpaRepository = riderProfileJpaRepository;
    }

    @PostMapping("/estimate")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Estimate fare for a standard or shared ride")
    public ResponseEntity<FareEstimateHttpResponse> estimate(@Valid @RequestBody FareEstimateHttpRequest request) {
        var result = rideApplicationService.estimateFare(new FareEstimateCommand(
                resolveAuthenticatedRiderProfileId(),
                request.rideType(),
                request.seatCount(),
                request.requestedVehicleType(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.pickupAddress(),
                request.dropLatitude(),
                request.dropLongitude(),
                request.dropAddress(),
                mapStops(request.stops())
        ));
        return ResponseEntity.ok(new FareEstimateHttpResponse(
                result.fareQuoteId(),
                result.rideType(),
                result.totalAmount(),
                result.currencyCode(),
                result.baseFare(),
                result.distanceFare(),
                result.durationFare(),
                result.surgeMultiplier(),
                result.bookingFee(),
                result.taxAmount(),
                result.poolingDiscountAmount(),
                result.quotedDistanceMeters(),
                result.quotedDurationSeconds(),
                result.expiresAt()
        ));
    }

    @PostMapping
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Book a ride with idempotent support")
    public ResponseEntity<RideBookingHttpResponse> bookRide(@Valid @RequestBody BookRideHttpRequest request) {
        var result = rideApplicationService.bookRide(new BookRideCommand(
                resolveAuthenticatedRiderProfileId(),
                request.rideType(),
                request.seatCount(),
                request.requestedVehicleType(),
                request.paymentMethodId(),
                request.pickupLatitude(),
                request.pickupLongitude(),
                request.pickupAddress(),
                request.dropLatitude(),
                request.dropLongitude(),
                request.dropAddress(),
                mapStops(request.stops()),
                request.notes()
        ));
        return ResponseEntity.ok(toResponse(result));
    }

    @PatchMapping("/{rideRequestId}/cancel")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Cancel a rider booking")
    public ResponseEntity<RideBookingHttpResponse> cancelRide(
            @PathVariable UUID rideRequestId,
            @Valid @RequestBody CancelRideHttpRequest request
    ) {
        var result = rideApplicationService.cancelRide(new CancelRideCommand(
                rideRequestId,
                resolveAuthenticatedRiderProfileId(),
                request.cancelReason()
        ));
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping("/{rideRequestId}")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Get ride details for a booking")
    public ResponseEntity<RideBookingHttpResponse> rideDetails(@PathVariable UUID rideRequestId) {
        return ResponseEntity.ok(toResponse(rideApplicationService.getRideDetails(
                rideRequestId,
                resolveAuthenticatedRiderProfileId()
        )));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Get recent ride history for the authenticated rider")
    public ResponseEntity<List<RideHistoryItemHttpResponse>> rideHistory() {
        List<RideHistoryItemResult> history = rideApplicationService.getRideHistory(resolveAuthenticatedRiderProfileId());
        return ResponseEntity.ok(history.stream()
                .map(item -> new RideHistoryItemHttpResponse(
                        item.rideRequestId(),
                        item.rideId(),
                        item.rideType(),
                        item.status(),
                        item.pickupAddress(),
                        item.dropAddress(),
                        item.amount(),
                        item.currencyCode(),
                        item.requestedAt()
                ))
                .toList());
    }

    private RideBookingHttpResponse toResponse(com.ridingplatform.ride.application.RideBookingResult result) {
        return new RideBookingHttpResponse(
                result.rideRequestId(),
                result.rideId(),
                result.riderProfileId(),
                result.rideType(),
                result.status(),
                result.fareQuoteId(),
                result.estimatedTotalAmount(),
                result.currencyCode(),
                result.requestedAt(),
                result.stops().stream()
                        .map(stop -> new RideStopHttpResponse(
                                stop.stopId(),
                                stop.stopType(),
                                stop.requestSequenceNo(),
                                stop.rideSequenceNo(),
                                stop.address()
                        ))
                        .toList()
        );
    }

    private UUID resolveAuthenticatedRiderProfileId() {
        UUID userProfileId = securityContextFacade.currentActor()
                .orElseThrow(() -> new com.ridingplatform.ride.application.RideValidationException("Authenticated actor not found"))
                .userProfileId();
        if (userProfileId == null) {
            throw new com.ridingplatform.ride.application.RideValidationException("user_profile_id claim is required");
        }
        return riderProfileJpaRepository.findByUserProfileId(userProfileId)
                .orElseThrow(() -> new com.ridingplatform.ride.application.RideValidationException("Rider profile not found for authenticated user"))
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
