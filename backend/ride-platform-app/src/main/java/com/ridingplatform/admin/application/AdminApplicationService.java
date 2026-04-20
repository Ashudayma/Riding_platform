package com.ridingplatform.admin.application;

import com.ridingplatform.admin.infrastructure.persistence.AdminAuditLogJpaRepository;
import com.ridingplatform.admin.infrastructure.persistence.AuditResultStatus;
import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import com.ridingplatform.notification.application.NotificationNotFoundException;
import com.ridingplatform.pricing.infrastructure.persistence.PricingRuleSetEntity;
import com.ridingplatform.pricing.infrastructure.persistence.PricingRuleSetJpaRepository;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import com.ridingplatform.rider.infrastructure.persistence.RiderStatus;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptStatus;
import com.ridingplatform.ride.infrastructure.persistence.RequestedRideType;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideLifecycleStatus;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import com.ridingplatform.ride.infrastructure.persistence.RideStatusHistoryJpaRepository;
import com.ridingplatform.security.application.AdminAuditService;
import com.ridingplatform.security.application.CurrentActor;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupJpaRepository;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminApplicationService {

    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository;
    private final RideJpaRepository rideJpaRepository;
    private final RiderProfileJpaRepository riderProfileJpaRepository;
    private final DriverProfileJpaRepository driverProfileJpaRepository;
    private final FraudFlagJpaRepository fraudFlagJpaRepository;
    private final FraudRiskProfileJpaRepository fraudRiskProfileJpaRepository;
    private final RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository;
    private final PricingRuleSetJpaRepository pricingRuleSetJpaRepository;
    private final DriverAvailabilityJpaRepository driverAvailabilityJpaRepository;
    private final SharedRideGroupJpaRepository sharedRideGroupJpaRepository;
    private final DriverAssignmentAttemptJpaRepository driverAssignmentAttemptJpaRepository;
    private final AdminAuditService adminAuditService;
    private final AdminAuditLogJpaRepository adminAuditLogJpaRepository;

    public AdminApplicationService(
            SpringDataRideRequestJpaRepository rideRequestJpaRepository,
            RideJpaRepository rideJpaRepository,
            RiderProfileJpaRepository riderProfileJpaRepository,
            DriverProfileJpaRepository driverProfileJpaRepository,
            FraudFlagJpaRepository fraudFlagJpaRepository,
            FraudRiskProfileJpaRepository fraudRiskProfileJpaRepository,
            RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository,
            PricingRuleSetJpaRepository pricingRuleSetJpaRepository,
            DriverAvailabilityJpaRepository driverAvailabilityJpaRepository,
            SharedRideGroupJpaRepository sharedRideGroupJpaRepository,
            DriverAssignmentAttemptJpaRepository driverAssignmentAttemptJpaRepository,
            AdminAuditService adminAuditService,
            AdminAuditLogJpaRepository adminAuditLogJpaRepository
    ) {
        this.rideRequestJpaRepository = rideRequestJpaRepository;
        this.rideJpaRepository = rideJpaRepository;
        this.riderProfileJpaRepository = riderProfileJpaRepository;
        this.driverProfileJpaRepository = driverProfileJpaRepository;
        this.fraudFlagJpaRepository = fraudFlagJpaRepository;
        this.fraudRiskProfileJpaRepository = fraudRiskProfileJpaRepository;
        this.rideStatusHistoryJpaRepository = rideStatusHistoryJpaRepository;
        this.pricingRuleSetJpaRepository = pricingRuleSetJpaRepository;
        this.driverAvailabilityJpaRepository = driverAvailabilityJpaRepository;
        this.sharedRideGroupJpaRepository = sharedRideGroupJpaRepository;
        this.driverAssignmentAttemptJpaRepository = driverAssignmentAttemptJpaRepository;
        this.adminAuditService = adminAuditService;
        this.adminAuditLogJpaRepository = adminAuditLogJpaRepository;
    }

    @Transactional(readOnly = true)
    public AdminPage<AdminRideView> searchRides(
            UUID riderProfileId,
            UUID driverProfileId,
            RideRequestStatusEntityType requestStatus,
            RideLifecycleStatus lifecycleStatus,
            RequestedRideType rideType,
            Instant from,
            Instant to,
            int page,
            int size,
            Sort sort
    ) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<RideRequestEntity> requestPage = rideRequestJpaRepository.searchAdmin(riderProfileId, requestStatus, rideType, from, to, pageable);
        List<AdminRideView> items = requestPage.getContent().stream()
                .map(request -> {
                    RideEntity ride = rideJpaRepository.findByBookingRequestId(request.getId()).orElse(null);
                    if (driverProfileId != null && (ride == null || ride.getDriverProfile() == null || !driverProfileId.equals(ride.getDriverProfile().getId()))) {
                        return null;
                    }
                    return toRideView(request, ride);
                })
                .filter(java.util.Objects::nonNull)
                .filter(view -> lifecycleStatus == null || lifecycleStatus.name().equals(view.lifecycleStatus()))
                .toList();
        return new AdminPage<>(items, requestPage.getTotalElements(), page, size, requestPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminRideView getRide(UUID rideRequestId) {
        RideRequestEntity request = rideRequestJpaRepository.findById(rideRequestId)
                .orElseThrow(() -> new AdminNotFoundException("Ride request not found"));
        return toRideView(request, rideJpaRepository.findByBookingRequestId(rideRequestId).orElse(null));
    }

    @Transactional(readOnly = true)
    public List<AdminRideTimelineItemView> rideTimeline(UUID rideRequestId) {
        return rideStatusHistoryJpaRepository.findByRideRequestIdOrderByChangedAtDesc(rideRequestId).stream()
                .map(item -> new AdminRideTimelineItemView(
                        item.getId(),
                        item.getPreviousStatus(),
                        item.getCurrentStatus(),
                        item.getSourceType().name(),
                        item.getActorType() == null ? null : item.getActorType().name(),
                        item.getNote(),
                        item.getChangedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminPage<AdminProfileView> riders(RiderStatus status, Boolean fraudHold, int page, int size, Sort sort) {
        Page<RiderProfileEntity> result = riderProfileJpaRepository.searchAdmin(status, fraudHold, PageRequest.of(page, size, sort));
        return new AdminPage<>(result.getContent().stream().map(this::toRiderView).toList(), result.getTotalElements(), page, size, result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminPage<AdminProfileView> drivers(DriverStatus status, Boolean fraudBlocked, int page, int size, Sort sort) {
        Page<DriverProfileEntity> result = driverProfileJpaRepository.searchAdmin(status, fraudBlocked, PageRequest.of(page, size, sort));
        return new AdminPage<>(result.getContent().stream().map(this::toDriverView).toList(), result.getTotalElements(), page, size, result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminPage<AdminFraudAlertView> fraudAlerts(FraudSubjectType subjectType, FraudFlagStatus flagStatus, int page, int size, Sort sort) {
        Page<com.ridingplatform.fraud.infrastructure.persistence.FraudFlagEntity> result =
                fraudFlagJpaRepository.searchAdmin(subjectType, flagStatus, PageRequest.of(page, size, sort));
        return new AdminPage<>(result.getContent().stream()
                .map(flag -> new AdminFraudAlertView(
                        flag.getId(),
                        flag.getSubjectType().name(),
                        flag.getSubjectId(),
                        flag.getSeverity().name(),
                        flag.getFlagStatus().name(),
                        flag.getRuleCode(),
                        flag.getRiskScore(),
                        flag.getTitle(),
                        flag.getCreatedAt()
                )).toList(), result.getTotalElements(), page, size, result.getTotalPages());
    }

    @Transactional
    public AdminProfileView blockRider(UUID riderProfileId, String reason, Optional<CurrentActor> actor, String requestId, String traceId, String ip, String userAgent) {
        RiderProfileEntity rider = riderProfileJpaRepository.findById(riderProfileId)
                .orElseThrow(() -> new AdminNotFoundException("Rider not found"));
        rider.setRiderStatus(RiderStatus.BLOCKED);
        rider.setFraudHold(true);
        riderProfileJpaRepository.save(rider);
        audit(actor, "admin.rider.block", "RIDER_PROFILE", riderProfileId, reason, requestId, traceId, ip, userAgent);
        return toRiderView(rider);
    }

    @Transactional
    public AdminProfileView unblockRider(UUID riderProfileId, String reason, Optional<CurrentActor> actor, String requestId, String traceId, String ip, String userAgent) {
        RiderProfileEntity rider = riderProfileJpaRepository.findById(riderProfileId)
                .orElseThrow(() -> new AdminNotFoundException("Rider not found"));
        rider.setRiderStatus(RiderStatus.ACTIVE);
        rider.setFraudHold(false);
        riderProfileJpaRepository.save(rider);
        audit(actor, "admin.rider.unblock", "RIDER_PROFILE", riderProfileId, reason, requestId, traceId, ip, userAgent);
        return toRiderView(rider);
    }

    @Transactional
    public AdminProfileView blockDriver(UUID driverProfileId, String reason, Optional<CurrentActor> actor, String requestId, String traceId, String ip, String userAgent) {
        DriverProfileEntity driver = driverProfileJpaRepository.findById(driverProfileId)
                .orElseThrow(() -> new AdminNotFoundException("Driver not found"));
        driver.setDriverStatus(DriverStatus.BLOCKED);
        driver.setFraudBlocked(true);
        driverProfileJpaRepository.save(driver);
        audit(actor, "admin.driver.block", "DRIVER_PROFILE", driverProfileId, reason, requestId, traceId, ip, userAgent);
        return toDriverView(driver);
    }

    @Transactional
    public AdminProfileView unblockDriver(UUID driverProfileId, String reason, Optional<CurrentActor> actor, String requestId, String traceId, String ip, String userAgent) {
        DriverProfileEntity driver = driverProfileJpaRepository.findById(driverProfileId)
                .orElseThrow(() -> new AdminNotFoundException("Driver not found"));
        driver.setDriverStatus(DriverStatus.ACTIVE);
        driver.setFraudBlocked(false);
        driverProfileJpaRepository.save(driver);
        audit(actor, "admin.driver.unblock", "DRIVER_PROFILE", driverProfileId, reason, requestId, traceId, ip, userAgent);
        return toDriverView(driver);
    }

    @Transactional(readOnly = true)
    public AdminPage<AdminPricingRuleView> pricingRules(String cityCode, Boolean active, int page, int size, Sort sort) {
        Page<PricingRuleSetEntity> result = pricingRuleSetJpaRepository.searchAdmin(cityCode, active, PageRequest.of(page, size, sort));
        return new AdminPage<>(result.getContent().stream().map(this::toPricingView).toList(), result.getTotalElements(), page, size, result.getTotalPages());
    }

    @Transactional
    public AdminPricingRuleView updatePricingRule(UUID pricingRuleId, boolean active, String reason, Optional<CurrentActor> actor, String requestId, String traceId, String ip, String userAgent) {
        PricingRuleSetEntity rule = pricingRuleSetJpaRepository.findById(pricingRuleId)
                .orElseThrow(() -> new AdminNotFoundException("Pricing rule not found"));
        rule.setActive(active);
        pricingRuleSetJpaRepository.save(rule);
        audit(actor, active ? "admin.pricing.activate" : "admin.pricing.deactivate", "PRICING_RULE", pricingRuleId, reason, requestId, traceId, ip, userAgent);
        return toPricingView(rule);
    }

    @Transactional(readOnly = true)
    public AdminOperationalMetricsView operationalMetrics() {
        long ridesInProgress = rideJpaRepository.searchAdmin(null, RideLifecycleStatus.IN_PROGRESS, null, null, null, PageRequest.of(0, 1)).getTotalElements();
        long ridesSearching = rideRequestJpaRepository.searchAdmin(null, RideRequestStatusEntityType.SEARCHING_DRIVER, null, null, null, PageRequest.of(0, 1)).getTotalElements();
        long availableDrivers = driverAvailabilityJpaRepository.searchNearbyPostgis(0, 0, 40075000, 100000, AvailabilityStatus.AVAILABLE.name(), null, false, null, false).size();
        long blockedDrivers = driverProfileJpaRepository.searchAdmin(null, true, PageRequest.of(0, 1)).getTotalElements();
        long blockedRiders = riderProfileJpaRepository.searchAdmin(null, true, PageRequest.of(0, 1)).getTotalElements();
        long openFraudAlerts = fraudFlagJpaRepository.searchAdmin(null, FraudFlagStatus.OPEN, PageRequest.of(0, 1)).getTotalElements();
        long openSharedGroups = sharedRideGroupJpaRepository.searchAdmin(SharedRideGroupStatus.OPEN, PageRequest.of(0, 1)).getTotalElements();
        return new AdminOperationalMetricsView(ridesInProgress, ridesSearching, availableDrivers, blockedDrivers, blockedRiders, openFraudAlerts, openSharedGroups);
    }

    @Transactional(readOnly = true)
    public AdminSharedRidePerformanceView sharedRidePerformance() {
        var total = sharedRideGroupJpaRepository.searchAdmin(null, PageRequest.of(0, 1000));
        long open = total.getContent().stream().filter(group -> group.getGroupStatus() == SharedRideGroupStatus.OPEN).count();
        long completed = total.getContent().stream().filter(group -> group.getGroupStatus() == SharedRideGroupStatus.COMPLETED).count();
        double avgSeatUtilization = total.getContent().stream()
                .filter(group -> group.getMaxSeatCapacity() > 0)
                .mapToDouble(group -> (double) group.getOccupiedSeatCount() / group.getMaxSeatCapacity())
                .average()
                .orElse(0.0d);
        BigDecimal savings = total.getContent().stream()
                .map(com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupEntity::getPoolingSavingsAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new AdminSharedRidePerformanceView(total.getTotalElements(), open, completed, round(avgSeatUtilization), savings);
    }

    @Transactional(readOnly = true)
    public AdminDispatchStatsView dispatchStats() {
        var attempts = driverAssignmentAttemptJpaRepository.findByDeletedAtIsNull(PageRequest.of(0, 1000)).getContent();
        return new AdminDispatchStatsView(
                attempts.size(),
                attempts.stream().filter(a -> a.getAssignmentStatus() == DriverAssignmentAttemptStatus.ACCEPTED).count(),
                attempts.stream().filter(a -> a.getAssignmentStatus() == DriverAssignmentAttemptStatus.REJECTED).count(),
                attempts.stream().filter(a -> a.getAssignmentStatus() == DriverAssignmentAttemptStatus.TIMED_OUT).count(),
                attempts.stream().filter(a -> a.getAssignmentStatus() == DriverAssignmentAttemptStatus.FAILED).count()
        );
    }

    @Transactional(readOnly = true)
    public AdminPage<com.ridingplatform.admin.infrastructure.persistence.AdminAuditLogEntity> auditLogs(String targetType, int page, int size) {
        Page<com.ridingplatform.admin.infrastructure.persistence.AdminAuditLogEntity> result = adminAuditLogJpaRepository.findByTargetTypeOrderByOccurredAtDesc(targetType, PageRequest.of(page, size));
        return new AdminPage<>(result.getContent(), result.getTotalElements(), page, size, result.getTotalPages());
    }

    private void audit(Optional<CurrentActor> actor, String actionCode, String targetType, UUID targetId, String reason, String requestId, String traceId, String ip, String userAgent) {
        adminAuditService.log(actor, actionCode, targetType, targetId, AuditResultStatus.SUCCESS, requestId, traceId, ip, userAgent,
                reason == null ? null : "{\"reason\":\"" + reason.replace("\"", "'") + "\"}");
    }

    private AdminRideView toRideView(RideRequestEntity request, RideEntity ride) {
        return new AdminRideView(
                request.getId(),
                ride == null ? null : ride.getId(),
                request.getRiderProfile().getId(),
                ride == null || ride.getDriverProfile() == null ? null : ride.getDriverProfile().getId(),
                request.getRequestedRideType().name(),
                request.getRequestStatus().name(),
                ride == null ? null : ride.getLifecycleStatus().name(),
                request.getOriginAddress(),
                request.getDestinationAddress(),
                request.getRequestedAt(),
                ride == null ? null : ride.getAssignedAt(),
                ride == null ? null : ride.getCompletedAt()
        );
    }

    private AdminProfileView toRiderView(RiderProfileEntity rider) {
        return new AdminProfileView(
                rider.getId(),
                rider.getUserProfile().getId(),
                rider.getRiderCode(),
                rider.getRiderStatus().name(),
                rider.getAverageRating(),
                BigDecimal.ZERO,
                rider.getRiderStatus() == RiderStatus.BLOCKED,
                rider.isFraudHold(),
                false,
                rider.getUserProfile().getDisplayName(),
                rider.getUserProfile().getEmail()
        );
    }

    private AdminProfileView toDriverView(DriverProfileEntity driver) {
        return new AdminProfileView(
                driver.getId(),
                driver.getUserProfile().getId(),
                driver.getDriverCode(),
                driver.getDriverStatus().name(),
                driver.getAverageRating(),
                driver.getRiskScore(),
                driver.getDriverStatus() == DriverStatus.BLOCKED,
                false,
                driver.isFraudBlocked(),
                driver.getUserProfile().getDisplayName(),
                driver.getUserProfile().getEmail()
        );
    }

    private AdminPricingRuleView toPricingView(PricingRuleSetEntity rule) {
        return new AdminPricingRuleView(
                rule.getId(),
                rule.getCityCode(),
                rule.getZoneCode(),
                rule.getRideType().name(),
                rule.getVehicleType() == null ? null : rule.getVehicleType().name(),
                rule.getPricingVersion(),
                rule.isActive(),
                rule.getBaseFare(),
                rule.getPerKmRate(),
                rule.getPerMinuteRate(),
                rule.getSharedDiscountFactor(),
                rule.getEffectiveFrom(),
                rule.getEffectiveTo()
        );
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }
}
