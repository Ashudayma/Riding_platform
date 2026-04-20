package com.ridingplatform.admin.interfaces;

import com.ridingplatform.admin.application.AdminActionRequest;
import com.ridingplatform.admin.application.AdminApplicationService;
import com.ridingplatform.admin.application.AdminPage;
import com.ridingplatform.admin.application.AdminPricingUpdateRequest;
import com.ridingplatform.driver.infrastructure.persistence.DriverStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import com.ridingplatform.ride.infrastructure.persistence.RequestedRideType;
import com.ridingplatform.ride.infrastructure.persistence.RideLifecycleStatus;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import com.ridingplatform.rider.infrastructure.persistence.RiderStatus;
import com.ridingplatform.security.application.SecurityContextFacade;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin")
public class AdminController {

    private final AdminApplicationService adminApplicationService;
    private final SecurityContextFacade securityContextFacade;

    public AdminController(AdminApplicationService adminApplicationService, SecurityContextFacade securityContextFacade) {
        this.adminApplicationService = adminApplicationService;
        this.securityContextFacade = securityContextFacade;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Get operational overview metrics for admins")
    public ResponseEntity<Map<String, Object>> overview() {
        var metrics = adminApplicationService.operationalMetrics();
        return ResponseEntity.ok(Map.of(
                "ridesInProgress", metrics.ridesInProgress(),
                "ridesSearchingDriver", metrics.ridesSearchingDriver(),
                "availableDrivers", metrics.availableDrivers(),
                "blockedDrivers", metrics.blockedDrivers(),
                "blockedRiders", metrics.blockedRiders(),
                "openFraudAlerts", metrics.openFraudAlerts(),
                "openSharedRideGroups", metrics.openSharedRideGroups()
        ));
    }

    @GetMapping("/rides")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Search rides with pagination, filters, and sorting")
    public ResponseEntity<AdminPageHttpResponse<?>> rides(
            @RequestParam(value = "riderProfileId", required = false) UUID riderProfileId,
            @RequestParam(value = "driverProfileId", required = false) UUID driverProfileId,
            @RequestParam(value = "requestStatus", required = false) RideRequestStatusEntityType requestStatus,
            @RequestParam(value = "lifecycleStatus", required = false) RideLifecycleStatus lifecycleStatus,
            @RequestParam(value = "rideType", required = false) RequestedRideType rideType,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "requestedAt,desc") String sort
    ) {
        return ResponseEntity.ok(toPage(adminApplicationService.searchRides(
                riderProfileId, driverProfileId, requestStatus, lifecycleStatus, rideType, from, to, page, size, parseSort(sort)
        )));
    }

    @GetMapping("/rides/{rideRequestId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN', 'SUPPORT_AGENT')")
    @Operation(summary = "Get ride details for an admin view")
    public ResponseEntity<?> ride(@PathVariable("rideRequestId") UUID rideRequestId) {
        return ResponseEntity.ok(adminApplicationService.getRide(rideRequestId));
    }

    @GetMapping("/rides/{rideRequestId}/timeline")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN', 'SUPPORT_AGENT', 'FRAUD_ANALYST')")
    @Operation(summary = "Get ride status timeline")
    public ResponseEntity<?> rideTimeline(@PathVariable("rideRequestId") UUID rideRequestId) {
        return ResponseEntity.ok(adminApplicationService.rideTimeline(rideRequestId));
    }

    @GetMapping("/riders")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN', 'SUPPORT_AGENT')")
    @Operation(summary = "Get rider profiles with filters")
    public ResponseEntity<AdminPageHttpResponse<?>> riders(
            @RequestParam(value = "status", required = false) RiderStatus status,
            @RequestParam(value = "fraudHold", required = false) Boolean fraudHold,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(toPage(adminApplicationService.riders(status, fraudHold, page, size, parseSort(sort))));
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN', 'SUPPORT_AGENT')")
    @Operation(summary = "Get driver profiles with filters")
    public ResponseEntity<AdminPageHttpResponse<?>> drivers(
            @RequestParam(value = "status", required = false) DriverStatus status,
            @RequestParam(value = "fraudBlocked", required = false) Boolean fraudBlocked,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(toPage(adminApplicationService.drivers(status, fraudBlocked, page, size, parseSort(sort))));
    }

    @GetMapping("/fraud/alerts")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN', 'FRAUD_ANALYST')")
    @Operation(summary = "Get fraud and risk alerts")
    public ResponseEntity<AdminPageHttpResponse<?>> fraudAlerts(
            @RequestParam(value = "subjectType", required = false) FraudSubjectType subjectType,
            @RequestParam(value = "flagStatus", required = false) FraudFlagStatus flagStatus,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(toPage(adminApplicationService.fraudAlerts(subjectType, flagStatus, page, size, parseSort(sort))));
    }

    @PatchMapping("/riders/{riderProfileId}/block")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Manually block a rider profile")
    public ResponseEntity<?> blockRider(@PathVariable("riderProfileId") UUID riderProfileId, @Valid @RequestBody AdminActionRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminApplicationService.blockRider(
                riderProfileId, request.reason(), securityContextFacade.currentActor(),
                httpRequest.getHeader("X-Request-Id"), httpRequest.getHeader("X-Trace-Id"), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")
        ));
    }

    @PatchMapping("/riders/{riderProfileId}/unblock")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Manually unblock a rider profile")
    public ResponseEntity<?> unblockRider(@PathVariable("riderProfileId") UUID riderProfileId, @Valid @RequestBody AdminActionRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminApplicationService.unblockRider(
                riderProfileId, request.reason(), securityContextFacade.currentActor(),
                httpRequest.getHeader("X-Request-Id"), httpRequest.getHeader("X-Trace-Id"), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")
        ));
    }

    @PatchMapping("/drivers/{driverProfileId}/block")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Manually block a driver profile")
    public ResponseEntity<?> blockDriver(@PathVariable("driverProfileId") UUID driverProfileId, @Valid @RequestBody AdminActionRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminApplicationService.blockDriver(
                driverProfileId, request.reason(), securityContextFacade.currentActor(),
                httpRequest.getHeader("X-Request-Id"), httpRequest.getHeader("X-Trace-Id"), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")
        ));
    }

    @PatchMapping("/drivers/{driverProfileId}/unblock")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Manually unblock a driver profile")
    public ResponseEntity<?> unblockDriver(@PathVariable("driverProfileId") UUID driverProfileId, @Valid @RequestBody AdminActionRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminApplicationService.unblockDriver(
                driverProfileId, request.reason(), securityContextFacade.currentActor(),
                httpRequest.getHeader("X-Request-Id"), httpRequest.getHeader("X-Trace-Id"), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")
        ));
    }

    @GetMapping("/pricing/rules")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Get pricing configurations")
    public ResponseEntity<AdminPageHttpResponse<?>> pricingRules(
            @RequestParam(value = "cityCode", required = false) String cityCode,
            @RequestParam(value = "active", required = false) Boolean active,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "effectiveFrom,desc") String sort
    ) {
        return ResponseEntity.ok(toPage(adminApplicationService.pricingRules(cityCode, active, page, size, parseSort(sort))));
    }

    @PutMapping("/pricing/rules/{pricingRuleId}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Activate or deactivate pricing configuration")
    public ResponseEntity<?> updatePricingRule(@PathVariable("pricingRuleId") UUID pricingRuleId, @Valid @RequestBody AdminPricingUpdateRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(adminApplicationService.updatePricingRule(
                pricingRuleId, request.active(), request.reason(), securityContextFacade.currentActor(),
                httpRequest.getHeader("X-Request-Id"), httpRequest.getHeader("X-Trace-Id"), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")
        ));
    }

    @GetMapping("/metrics/operations")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Get live operational metrics")
    public ResponseEntity<?> operationalMetrics() {
        return ResponseEntity.ok(adminApplicationService.operationalMetrics());
    }

    @GetMapping("/shared-rides/performance")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Review shared ride performance")
    public ResponseEntity<?> sharedRidePerformance() {
        return ResponseEntity.ok(adminApplicationService.sharedRidePerformance());
    }

    @GetMapping("/dispatch/stats")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Get dispatch and assignment stats")
    public ResponseEntity<?> dispatchStats() {
        return ResponseEntity.ok(adminApplicationService.dispatchStats());
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'OPS_ADMIN')")
    @Operation(summary = "Get admin audit logs by target type")
    public ResponseEntity<AdminPageHttpResponse<AdminAuditLogHttpResponse>> auditLogs(
            @RequestParam("targetType") String targetType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        AdminPage<com.ridingplatform.admin.infrastructure.persistence.AdminAuditLogEntity> logs = adminApplicationService.auditLogs(targetType, page, size);
        return ResponseEntity.ok(new AdminPageHttpResponse<>(
                logs.items().stream().map(item -> new AdminAuditLogHttpResponse(
                        item.getId(), item.getActionCode(), item.getTargetType(), item.getTargetId(),
                        item.getResultStatus().name(), item.getRequestId(), item.getTraceId(), item.getOccurredAt()
                )).toList(),
                logs.totalElements(), logs.page(), logs.size(), logs.totalPages()
        ));
    }

    private Sort parseSort(String value) {
        String[] parts = value.split(",", 2);
        String property = parts[0];
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private <T> AdminPageHttpResponse<T> toPage(AdminPage<T> page) {
        return new AdminPageHttpResponse<>(page.items(), page.totalElements(), page.page(), page.size(), page.totalPages());
    }
}
