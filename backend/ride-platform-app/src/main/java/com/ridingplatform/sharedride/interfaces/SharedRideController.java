package com.ridingplatform.sharedride.interfaces;

import com.ridingplatform.ride.application.RideValidationException;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import com.ridingplatform.security.application.SecurityContextFacade;
import com.ridingplatform.sharedride.application.PoolMatchDecision;
import com.ridingplatform.sharedride.application.SharedRideMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shared-rides")
@Tag(name = "Shared Ride Matching")
public class SharedRideController {

    private final SharedRideMatchingService sharedRideMatchingService;
    private final SecurityContextFacade securityContextFacade;
    private final RiderProfileJpaRepository riderProfileJpaRepository;
    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository;

    public SharedRideController(
            SharedRideMatchingService sharedRideMatchingService,
            SecurityContextFacade securityContextFacade,
            RiderProfileJpaRepository riderProfileJpaRepository,
            SpringDataRideRequestJpaRepository rideRequestJpaRepository
    ) {
        this.sharedRideMatchingService = sharedRideMatchingService;
        this.securityContextFacade = securityContextFacade;
        this.riderProfileJpaRepository = riderProfileJpaRepository;
        this.rideRequestJpaRepository = rideRequestJpaRepository;
    }

    @GetMapping("/{rideRequestId}/preview")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Preview best pooling candidate for a shared ride request")
    public ResponseEntity<SharedRidePreviewHttpResponse> preview(@PathVariable UUID rideRequestId) {
        ensureOwnership(rideRequestId);
        return ResponseEntity.ok(toResponse(sharedRideMatchingService.previewMatch(rideRequestId).orElse(
                new PoolMatchDecision(false, null, null, null, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, "NO_POOLING_CANDIDATE", null, null)
        )));
    }

    @PostMapping("/{rideRequestId}/match")
    @PreAuthorize("hasRole('RIDER')")
    @Operation(summary = "Trigger shared ride matching for a rider booking")
    public ResponseEntity<SharedRidePreviewHttpResponse> match(@PathVariable UUID rideRequestId) {
        ensureOwnership(rideRequestId);
        return ResponseEntity.ok(toResponse(sharedRideMatchingService.matchOrCreateGroup(rideRequestId)));
    }

    private void ensureOwnership(UUID rideRequestId) {
        UUID userProfileId = securityContextFacade.currentActor()
                .orElseThrow(() -> new RideValidationException("Authenticated actor not found"))
                .userProfileId();
        UUID riderProfileId = riderProfileJpaRepository.findByUserProfileId(userProfileId)
                .orElseThrow(() -> new RideValidationException("Rider profile not found for authenticated user"))
                .getId();
        boolean belongsToRider = rideRequestJpaRepository.findById(rideRequestId)
                .map(rideRequest -> rideRequest.getRiderProfile().getId().equals(riderProfileId))
                .orElse(false);
        if (!belongsToRider) {
            throw new RideValidationException("Ride request does not belong to authenticated rider");
        }
    }

    private SharedRidePreviewHttpResponse toResponse(PoolMatchDecision decision) {
        return new SharedRidePreviewHttpResponse(
                decision.matched(),
                decision.sharedRideGroupId(),
                decision.anchorRideRequestId(),
                decision.anchorRideId(),
                decision.compatibilityScore(),
                decision.estimatedSavingsAmount(),
                decision.rejectionReason(),
                decision.routePlan() == null ? null : decision.routePlan().permutationCode(),
                decision.routePlan() == null ? null : decision.routePlan().totalDurationSeconds(),
                decision.routePlan() == null ? null : decision.routePlan().totalDistanceMeters(),
                decision.routePlan() == null ? List.of() : mapStops(decision)
        );
    }

    private List<SharedRideRouteStopHttpResponse> mapStops(PoolMatchDecision decision) {
        List<com.ridingplatform.sharedride.application.PoolRouteStop> orderedStops = decision.routePlan().orderedStops();
        java.util.ArrayList<SharedRideRouteStopHttpResponse> result = new java.util.ArrayList<>();
        int index = 1;
        for (com.ridingplatform.sharedride.application.PoolRouteStop stop : orderedStops) {
            result.add(new SharedRideRouteStopHttpResponse(
                    stop.rideRequestId(),
                    stop.riderProfileId(),
                    stop.stopType(),
                    stop.role(),
                    index++,
                    stop.address()
            ));
        }
        return result;
    }
}
