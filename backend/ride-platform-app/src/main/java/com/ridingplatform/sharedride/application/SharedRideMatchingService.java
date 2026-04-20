package com.ridingplatform.sharedride.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteJpaRepository;
import com.ridingplatform.ride.application.RideNotFoundException;
import com.ridingplatform.ride.application.RideValidationException;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RequestedRideType;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import com.ridingplatform.ride.infrastructure.persistence.RideStopEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideStopJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.StopType;
import com.ridingplatform.sharedride.domain.PoolConstraintPolicy;
import com.ridingplatform.sharedride.infrastructure.persistence.PoolingEvaluationStatus;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideCandidateEntity;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideCandidateJpaRepository;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupEntity;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupJpaRepository;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupStatus;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideRouteStopEntity;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideRouteStopJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SharedRideMatchingService {

    private final ApplicationProperties applicationProperties;
    private final SharedRideGroupJpaRepository sharedRideGroupJpaRepository;
    private final SharedRideCandidateJpaRepository sharedRideCandidateJpaRepository;
    private final SharedRideRouteStopJpaRepository sharedRideRouteStopJpaRepository;
    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository;
    private final RideJpaRepository rideJpaRepository;
    private final RideStopJpaRepository rideStopJpaRepository;
    private final FareQuoteJpaRepository fareQuoteJpaRepository;
    private final PoolRouteEvaluator poolRouteEvaluator;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public SharedRideMatchingService(
            ApplicationProperties applicationProperties,
            SharedRideGroupJpaRepository sharedRideGroupJpaRepository,
            SharedRideCandidateJpaRepository sharedRideCandidateJpaRepository,
            SharedRideRouteStopJpaRepository sharedRideRouteStopJpaRepository,
            SpringDataRideRequestJpaRepository rideRequestJpaRepository,
            RideJpaRepository rideJpaRepository,
            RideStopJpaRepository rideStopJpaRepository,
            FareQuoteJpaRepository fareQuoteJpaRepository,
            PoolRouteEvaluator poolRouteEvaluator,
            IdGenerator idGenerator,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.applicationProperties = applicationProperties;
        this.sharedRideGroupJpaRepository = sharedRideGroupJpaRepository;
        this.sharedRideCandidateJpaRepository = sharedRideCandidateJpaRepository;
        this.sharedRideRouteStopJpaRepository = sharedRideRouteStopJpaRepository;
        this.rideRequestJpaRepository = rideRequestJpaRepository;
        this.rideJpaRepository = rideJpaRepository;
        this.rideStopJpaRepository = rideStopJpaRepository;
        this.fareQuoteJpaRepository = fareQuoteJpaRepository;
        this.poolRouteEvaluator = poolRouteEvaluator;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PoolMatchDecision matchOrCreateGroup(UUID newRideRequestId) {
        RideRequestEntity newRideRequest = rideRequestJpaRepository.findByIdForUpdate(newRideRequestId)
                .orElseThrow(() -> new RideNotFoundException("Ride request not found"));
        if (newRideRequest.getRequestedRideType() != RequestedRideType.SHARED) {
            throw new RideValidationException("Pooling can only be evaluated for shared ride requests");
        }
        RideEntity newRide = rideJpaRepository.findByBookingRequestIdForUpdate(newRideRequestId)
                .orElseThrow(() -> new RideNotFoundException("Ride not found for ride request"));
        PoolConstraintPolicy policy = new PoolConstraintPolicy(
                applicationProperties.sharedRide().maxDetourMinutes(),
                applicationProperties.sharedRide().maxPickupStops(),
                applicationProperties.sharedRide().maxDropStops(),
                applicationProperties.sharedRide().maxPickupWaitMinutes(),
                applicationProperties.sharedRide().maxDetourPercentage(),
                applicationProperties.sharedRide().maxPoolSearchRadiusMeters(),
                applicationProperties.sharedRide().baseCompatibilityThreshold()
        );

        PoolMatchDecision bestDecision = null;
        for (SharedRideGroupEntity group : sharedRideGroupJpaRepository.findOpenGroups(Set.of(SharedRideGroupStatus.FORMING, SharedRideGroupStatus.OPEN))) {
            if (group.getAnchorRideRequest() == null || group.getAnchorRideRequest().getId().equals(newRideRequestId)) {
                continue;
            }
            Optional<PoolMatchDecision> decision = evaluateAgainstGroup(group, newRideRequest, policy);
            if (decision.isPresent() && (bestDecision == null || decision.get().compatibilityScore().compareTo(bestDecision.compatibilityScore()) > 0)) {
                bestDecision = decision.get();
            }
        }

        if (bestDecision == null) {
            SharedRideGroupEntity group = ensureStandaloneGroup(newRideRequest);
            newRide.setSharedRideGroup(group);
            rideJpaRepository.save(newRide);
            return new PoolMatchDecision(false, group.getId(), newRideRequest.getId(), newRide.getId(), BigDecimal.ZERO,
                    BigDecimal.ZERO, "NO_POOLING_CANDIDATE", null, null);
        }

        SharedRideGroupEntity lockedGroup = sharedRideGroupJpaRepository.findByIdForUpdate(bestDecision.sharedRideGroupId())
                .orElseThrow(() -> new RideNotFoundException("Shared ride group not found"));
        applyMatch(lockedGroup, newRideRequest, newRide, bestDecision);
        return bestDecision;
    }

    @Transactional(readOnly = true)
    public Optional<PoolMatchDecision> previewMatch(UUID rideRequestId) {
        RideRequestEntity rideRequest = rideRequestJpaRepository.findById(rideRequestId)
                .orElseThrow(() -> new RideNotFoundException("Ride request not found"));
        PoolConstraintPolicy policy = new PoolConstraintPolicy(
                applicationProperties.sharedRide().maxDetourMinutes(),
                applicationProperties.sharedRide().maxPickupStops(),
                applicationProperties.sharedRide().maxDropStops(),
                applicationProperties.sharedRide().maxPickupWaitMinutes(),
                applicationProperties.sharedRide().maxDetourPercentage(),
                applicationProperties.sharedRide().maxPoolSearchRadiusMeters(),
                applicationProperties.sharedRide().baseCompatibilityThreshold()
        );
        PoolMatchDecision bestDecision = null;
        for (SharedRideGroupEntity group : sharedRideGroupJpaRepository.findOpenGroups(Set.of(SharedRideGroupStatus.FORMING, SharedRideGroupStatus.OPEN))) {
            if (group.getAnchorRideRequest() == null || group.getAnchorRideRequest().getId().equals(rideRequestId)) {
                continue;
            }
            Optional<PoolMatchDecision> decision = evaluateAgainstGroup(group, rideRequest, policy);
            if (decision.isPresent() && (bestDecision == null || decision.get().compatibilityScore().compareTo(bestDecision.compatibilityScore()) > 0)) {
                bestDecision = decision.get();
            }
        }
        return Optional.ofNullable(bestDecision);
    }

    private Optional<PoolMatchDecision> evaluateAgainstGroup(
            SharedRideGroupEntity group,
            RideRequestEntity newRideRequest,
            PoolConstraintPolicy policy
    ) {
        RideRequestEntity existingRideRequest = rideRequestJpaRepository.findById(group.getAnchorRideRequest().getId())
                .orElseThrow(() -> new RideNotFoundException("Anchor ride request not found"));
        if (group.getOccupiedSeatCount() + newRideRequest.getSeatCount() > group.getMaxSeatCapacity()) {
            persistCandidate(existingRideRequest, newRideRequest, group, PoolingEvaluationStatus.INCOMPATIBLE, null, "SEAT_CAPACITY_EXCEEDED", BigDecimal.ZERO);
            return Optional.empty();
        }

        List<RideStopEntity> existingStops = rideStopJpaRepository.findByRideRequestIdOrderByRequestSequenceNoAsc(existingRideRequest.getId());
        List<RideStopEntity> newStops = rideStopJpaRepository.findByRideRequestIdOrderByRequestSequenceNoAsc(newRideRequest.getId());
        if (existingStops.size() < 2 || newStops.size() < 2) {
            return Optional.empty();
        }

        PoolRouteEvaluationContext context = new PoolRouteEvaluationContext(
                policy,
                group.getMaxSeatCapacity(),
                group.getOccupiedSeatCount(),
                toPoolStop(existingStops.get(0), RouteStopRole.PICKUP_EXISTING),
                toPoolStop(existingStops.get(existingStops.size() - 1), RouteStopRole.DROPOFF_EXISTING),
                toPoolStop(newStops.get(0), RouteStopRole.PICKUP_NEW),
                toPoolStop(newStops.get(newStops.size() - 1), RouteStopRole.DROPOFF_NEW)
        );
        List<PoolRoutePlan> plans = poolRouteEvaluator.evaluate(context);
        if (plans.isEmpty()) {
            persistCandidate(existingRideRequest, newRideRequest, group, PoolingEvaluationStatus.INCOMPATIBLE, null, "NO_FEASIBLE_ROUTE", BigDecimal.ZERO);
            return Optional.empty();
        }
        PoolRoutePlan bestPlan = plans.get(0);
        PoolCompatibilityBreakdown breakdown = compatibilityBreakdown(bestPlan, group.getMaxSeatCapacity());
        BigDecimal compatibilityScore = BigDecimal.valueOf(
                breakdown.routeOverlapScore()
                        + breakdown.waitTimeScore()
                        + breakdown.detourScore()
                        + breakdown.seatUtilizationScore()
                        + breakdown.riderCompatibilityScore()
        ).setScale(4, RoundingMode.HALF_UP);
        if (compatibilityScore.doubleValue() < policy.baseCompatibilityThreshold()) {
            persistCandidate(existingRideRequest, newRideRequest, group, PoolingEvaluationStatus.INCOMPATIBLE, bestPlan, "LOW_COMPATIBILITY_SCORE", compatibilityScore);
            return Optional.empty();
        }
        BigDecimal savings = estimateSavings(existingRideRequest, newRideRequest);
        persistCandidate(existingRideRequest, newRideRequest, group, PoolingEvaluationStatus.COMPATIBLE, bestPlan, null, compatibilityScore);
        return Optional.of(new PoolMatchDecision(
                true,
                group.getId(),
                existingRideRequest.getId(),
                rideJpaRepository.findByBookingRequestId(existingRideRequest.getId()).map(RideEntity::getId).orElse(null),
                compatibilityScore,
                savings,
                null,
                bestPlan,
                breakdown
        ));
    }

    private SharedRideGroupEntity ensureStandaloneGroup(RideRequestEntity rideRequest) {
        return sharedRideGroupJpaRepository.findOpenGroups(Set.of(SharedRideGroupStatus.FORMING, SharedRideGroupStatus.OPEN)).stream()
                .filter(group -> group.getAnchorRideRequest() != null && group.getAnchorRideRequest().getId().equals(rideRequest.getId()))
                .findFirst()
                .orElseGet(() -> {
                    SharedRideGroupEntity group = new SharedRideGroupEntity();
                    group.setId(idGenerator.nextId());
                    group.setGroupStatus(SharedRideGroupStatus.FORMING);
                    group.setAnchorRideRequest(rideRequest);
                    group.setMaxSeatCapacity((short) Math.max(4, rideRequest.getSeatCount() + 1));
                    group.setOccupiedSeatCount(rideRequest.getSeatCount());
                    group.setPoolingSavingsAmount(BigDecimal.ZERO);
                    group.setFormedAt(Instant.now(clock));
                    return sharedRideGroupJpaRepository.save(group);
                });
    }

    private void applyMatch(
            SharedRideGroupEntity group,
            RideRequestEntity newRideRequest,
            RideEntity newRide,
            PoolMatchDecision decision
    ) {
        RideEntity anchorRide = rideJpaRepository.findByBookingRequestIdForUpdate(decision.anchorRideRequestId())
                .orElseThrow(() -> new RideNotFoundException("Anchor ride not found"));
        RideRequestEntity anchorRequest = rideRequestJpaRepository.findByIdForUpdate(decision.anchorRideRequestId())
                .orElseThrow(() -> new RideNotFoundException("Anchor ride request not found"));

        group.setGroupStatus(SharedRideGroupStatus.OPEN);
        group.setOccupiedSeatCount((short) (group.getOccupiedSeatCount() + newRideRequest.getSeatCount()));
        group.setRouteDistanceMeters(decision.routePlan().totalDistanceMeters());
        group.setRouteDurationSeconds(decision.routePlan().totalDurationSeconds());
        group.setDetourSeconds(Math.max(decision.routePlan().existingRiderDetourSeconds(), decision.routePlan().newRiderDetourSeconds()));
        group.setPoolingSavingsAmount(group.getPoolingSavingsAmount().add(decision.estimatedSavingsAmount()));
        sharedRideGroupJpaRepository.save(group);

        anchorRide.setSharedRideGroup(group);
        newRide.setSharedRideGroup(group);
        newRide.setDriverProfile(anchorRide.getDriverProfile());
        newRide.setVehicle(anchorRide.getVehicle());
        newRide.setAssignedAt(anchorRide.getAssignedAt());
        rideJpaRepository.save(anchorRide);
        rideJpaRepository.save(newRide);

        applyPoolingDiscount(anchorRequest, decision.estimatedSavingsAmount().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP));
        applyPoolingDiscount(newRideRequest, decision.estimatedSavingsAmount().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP));
        persistRoute(group, decision.routePlan());
        syncRideStopSequence(anchorRide.getId(), decision.routePlan());
    }

    private void persistRoute(SharedRideGroupEntity group, PoolRoutePlan routePlan) {
        sharedRideRouteStopJpaRepository.deleteBySharedRideGroupId(group.getId());
        List<SharedRideRouteStopEntity> routeStops = new ArrayList<>();
        int sequence = 1;
        for (PoolRouteStop stop : routePlan.orderedStops()) {
            SharedRideRouteStopEntity entity = new SharedRideRouteStopEntity();
            entity.setId(idGenerator.nextId());
            entity.setSharedRideGroup(group);
            entity.setRideRequest(rideRequestJpaRepository.getReferenceById(stop.rideRequestId()));
            entity.setRiderProfile(rideRequestJpaRepository.getReferenceById(stop.rideRequestId()).getRiderProfile());
            entity.setStopType(stop.stopType());
            entity.setRouteSequenceNo(sequence++);
            entity.setStopPoint(new org.locationtech.jts.geom.GeometryFactory().createPoint(new org.locationtech.jts.geom.Coordinate(stop.longitude(), stop.latitude())));
            entity.setAddressLine(stop.address());
            entity.setLocality(stop.locality());
            entity.setPassengerCount(stop.passengerCount());
            entity.setMetadataJson(serialize(Map.of("role", stop.role().name())));
            routeStops.add(entity);
        }
        sharedRideRouteStopJpaRepository.saveAll(routeStops);
    }

    private void syncRideStopSequence(UUID operationalRideId, PoolRoutePlan routePlan) {
        Map<String, Integer> sequenceMap = new java.util.HashMap<>();
        int sequence = 1;
        for (PoolRouteStop stop : routePlan.orderedStops()) {
            sequenceMap.put(stop.rideRequestId() + ":" + stop.stopType().name(), sequence++);
        }
        List<UUID> rideRequestIds = routePlan.orderedStops().stream().map(PoolRouteStop::rideRequestId).distinct().toList();
        for (UUID rideRequestId : rideRequestIds) {
            List<RideStopEntity> stops = rideStopJpaRepository.findByRideRequestIdOrderByRequestSequenceNoAsc(rideRequestId);
            for (RideStopEntity stop : stops) {
                String key = rideRequestId + ":" + stop.getStopType().name();
                if (sequenceMap.containsKey(key)) {
                    stop.setRide(rideJpaRepository.getReferenceById(operationalRideId));
                    stop.setRideSequenceNo(sequenceMap.get(key));
                    stop.setDetourSeconds(detourFor(stop, routePlan));
                }
            }
            rideStopJpaRepository.saveAll(stops);
        }
    }

    private Integer detourFor(RideStopEntity stop, PoolRoutePlan plan) {
        UUID existingRideRequestId = plan.orderedStops().stream()
                .filter(routeStop -> routeStop.role() == RouteStopRole.PICKUP_EXISTING)
                .map(PoolRouteStop::rideRequestId)
                .findFirst()
                .orElse(null);
        return switch (stop.getStopType()) {
            case PICKUP -> stop.getRideRequest().getId().equals(existingRideRequestId)
                    ? plan.existingRiderPickupWaitSeconds()
                    : plan.newRiderPickupWaitSeconds();
            case DROPOFF -> stop.getRideRequest().getId().equals(existingRideRequestId)
                    ? plan.existingRiderDetourSeconds()
                    : plan.newRiderDetourSeconds();
            default -> 0;
        };
    }

    private void applyPoolingDiscount(RideRequestEntity rideRequest, BigDecimal additionalDiscount) {
        FareQuoteEntity quote = rideRequest.getFareQuote();
        if (quote == null) {
            return;
        }
        BigDecimal updatedPoolingDiscount = quote.getPoolingDiscountAmount().add(additionalDiscount);
        quote.setPoolingDiscountAmount(updatedPoolingDiscount);
        quote.setDiscountAmount(quote.getDiscountAmount().add(additionalDiscount));
        quote.setTotalAmount(quote.getTotalAmount().subtract(additionalDiscount).max(new BigDecimal("1.00")));
        quote.setPricingStrategyCode("SHARED_POOL_MATCH_V2");
        fareQuoteJpaRepository.save(quote);
    }

    private BigDecimal estimateSavings(RideRequestEntity existingRideRequest, RideRequestEntity newRideRequest) {
        BigDecimal existing = existingRideRequest.getFareQuote() == null ? BigDecimal.ZERO : existingRideRequest.getFareQuote().getTotalAmount();
        BigDecimal incoming = newRideRequest.getFareQuote() == null ? BigDecimal.ZERO : newRideRequest.getFareQuote().getTotalAmount();
        return existing.add(incoming)
                .multiply(BigDecimal.valueOf(applicationProperties.sharedRide().pricingDiscountFactor()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PoolCompatibilityBreakdown compatibilityBreakdown(PoolRoutePlan plan, short seatCapacity) {
        double routeOverlap = normalizeInverse(plan.totalDurationSeconds(), applicationProperties.sharedRide().maxDetourMinutes() * 60 * 3L);
        double wait = (normalizeInverse(plan.existingRiderPickupWaitSeconds(), applicationProperties.sharedRide().maxPickupWaitMinutes() * 60.0d)
                + normalizeInverse(plan.newRiderPickupWaitSeconds(), applicationProperties.sharedRide().maxPickupWaitMinutes() * 60.0d)) / 2.0d;
        double detour = (normalizeInverse(plan.existingRiderDetourSeconds(), applicationProperties.sharedRide().maxDetourMinutes() * 60.0d)
                + normalizeInverse(plan.newRiderDetourSeconds(), applicationProperties.sharedRide().maxDetourMinutes() * 60.0d)) / 2.0d;
        double seatUtilization = Math.min(1.0d, (double) plan.peakSeatUsage() / Math.max(1, seatCapacity));
        double riderCompatibility = normalizeInverse(Math.abs(plan.detourPercentageExisting() - plan.detourPercentageNew()), 100.0d);
        return new PoolCompatibilityBreakdown(
                round(routeOverlap * 0.30d),
                round(wait * 0.20d),
                round(detour * 0.25d),
                round(seatUtilization * 0.10d),
                round(riderCompatibility * 0.15d)
        );
    }

    private void persistCandidate(
            RideRequestEntity baseRideRequest,
            RideRequestEntity candidateRideRequest,
            SharedRideGroupEntity group,
            PoolingEvaluationStatus status,
            PoolRoutePlan routePlan,
            String rejectionReason,
            BigDecimal compatibilityScore
    ) {
        SharedRideCandidateEntity entity = new SharedRideCandidateEntity();
        entity.setId(idGenerator.nextId());
        entity.setBaseRideRequest(baseRideRequest);
        entity.setCandidateRideRequest(candidateRideRequest);
        entity.setProposedGroup(group);
        entity.setEvaluationStatus(status);
        entity.setCompatibilityScore(compatibilityScore == null ? BigDecimal.ZERO : compatibilityScore);
        entity.setOverlapDistanceMeters(routePlan == null ? null : routePlan.totalDistanceMeters());
        entity.setDetourDeltaSeconds(routePlan == null ? null : Math.max(routePlan.existingRiderDetourSeconds(), routePlan.newRiderDetourSeconds()));
        entity.setEstimatedSavingsAmount(routePlan == null ? BigDecimal.ZERO : estimateSavings(baseRideRequest, candidateRideRequest));
        entity.setSeatFit(routePlan != null && routePlan.peakSeatUsage() <= group.getMaxSeatCapacity());
        entity.setRejectionReason(rejectionReason);
        entity.setEvaluationMetadataJson(routePlan == null ? null : serialize(Map.of(
                "permutationCode", routePlan.permutationCode(),
                "totalDurationSeconds", routePlan.totalDurationSeconds(),
                "totalDistanceMeters", routePlan.totalDistanceMeters(),
                "existingDetourSeconds", routePlan.existingRiderDetourSeconds(),
                "newDetourSeconds", routePlan.newRiderDetourSeconds()
        )));
        entity.setEvaluatedAt(Instant.now(clock));
        entity.setExpiresAt(Instant.now(clock).plusSeconds(600));
        sharedRideCandidateJpaRepository.save(entity);
    }

    private PoolRouteStop toPoolStop(RideStopEntity stop, RouteStopRole role) {
        return new PoolRouteStop(
                stop.getRideRequest().getId(),
                stop.getRiderProfile().getId(),
                stop.getStopType(),
                role,
                stop.getStopPoint().getY(),
                stop.getStopPoint().getX(),
                stop.getAddressLine(),
                stop.getLocality(),
                stop.getPassengerCount()
        );
    }

    private double normalizeInverse(double value, double max) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, 1.0d - Math.min(value, max) / max);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
