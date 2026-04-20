package com.ridingplatform.driver.interfaces;

import com.ridingplatform.driver.application.DriverAvailabilityCommand;
import com.ridingplatform.driver.application.DriverAvailabilityService;
import com.ridingplatform.driver.application.DriverLocationUpdateCommand;
import com.ridingplatform.driver.application.NearbyDriverSearchCommand;
import com.ridingplatform.security.application.SecurityContextFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/driver")
@Tag(name = "Driver Availability")
public class DriverOperationsController {

    private final SecurityContextFacade securityContextFacade;
    private final DriverAvailabilityService driverAvailabilityService;

    public DriverOperationsController(
            SecurityContextFacade securityContextFacade,
            DriverAvailabilityService driverAvailabilityService
    ) {
        this.securityContextFacade = securityContextFacade;
        this.driverAvailabilityService = driverAvailabilityService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('DRIVER', 'SUPPORT_AGENT', 'OPS_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, Object>> me() {
        var actor = securityContextFacade.currentActor().orElseThrow();
        return ResponseEntity.ok(Map.of(
                "subject", actor.subject(),
                "userProfileId", actor.userProfileId(),
                "roles", actor.roles()
        ));
    }

    @PatchMapping("/availability")
    @PreAuthorize("hasAnyRole('DRIVER', 'OPS_ADMIN')")
    @Operation(summary = "Mark driver online or offline")
    public ResponseEntity<DriverAvailabilityHttpResponse> updateAvailability(@Valid @RequestBody DriverAvailabilityRequest request) {
        var result = driverAvailabilityService.updateAvailability(new DriverAvailabilityCommand(
                request.online(),
                request.availableSeatCount(),
                request.sessionId(),
                request.appVersion(),
                request.devicePlatform()
        ));
        return ResponseEntity.ok(new DriverAvailabilityHttpResponse(
                result.driverProfileId(),
                result.availabilityStatus(),
                result.onlineStatus(),
                result.availableSeatCount(),
                result.currentRideId(),
                result.latitude(),
                result.longitude(),
                result.accuracyMeters(),
                result.lastHeartbeatAt()
        ));
    }

    @PostMapping("/location")
    @PreAuthorize("hasAnyRole('DRIVER', 'OPS_ADMIN')")
    @Operation(summary = "Update live driver location")
    public ResponseEntity<DriverLocationHttpResponse> updateLocation(@Valid @RequestBody DriverLocationUpdateRequest request) {
        var result = driverAvailabilityService.updateLiveLocation(new DriverLocationUpdateCommand(
                request.latitude(),
                request.longitude(),
                request.headingDegrees(),
                request.speedKph(),
                request.accuracyMeters(),
                request.locationProvider()
        ));
        return ResponseEntity.ok(new DriverLocationHttpResponse(
                result.driverProfileId(),
                result.latitude(),
                result.longitude(),
                result.observedAt()
        ));
    }

    @PostMapping("/search/nearby")
    @PreAuthorize("hasAnyRole('OPS_ADMIN', 'PLATFORM_ADMIN', 'SUPPORT_AGENT')")
    @Operation(summary = "Search nearby drivers with operational filters")
    public ResponseEntity<List<NearbyDriverHttpResponse>> searchNearby(@Valid @RequestBody NearbyDriverSearchRequest request) {
        return ResponseEntity.ok(driverAvailabilityService.searchNearby(new NearbyDriverSearchCommand(
                        request.latitude(),
                        request.longitude(),
                        request.radiusMeters(),
                        request.limit(),
                        request.availabilityStatus(),
                        request.vehicleType(),
                        request.excludeActiveRide(),
                        request.minimumRating(),
                        request.excludeRiskBlocked()
                )).stream()
                .map(result -> new NearbyDriverHttpResponse(
                        result.driverProfileId(),
                        result.vehicleId(),
                        result.vehicleType(),
                        result.availabilityStatus(),
                        result.rating(),
                        result.riskBlocked(),
                        result.currentRideId(),
                        result.latitude(),
                        result.longitude(),
                        result.distanceMeters(),
                        result.lastHeartbeatAt()
                ))
                .toList());
    }
}
