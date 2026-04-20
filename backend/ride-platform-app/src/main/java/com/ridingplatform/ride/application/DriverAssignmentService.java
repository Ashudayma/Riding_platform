package com.ridingplatform.ride.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.driver.application.DriverAvailabilityService;
import com.ridingplatform.driver.application.NearbyDriverResult;
import com.ridingplatform.driver.application.NearbyDriverSearchCommand;
import com.ridingplatform.driver.application.DriverDispatchCandidate;
import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.OnlineStatus;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptEntity;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptStatus;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentFailureReason;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideLifecycleStatus;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import com.ridingplatform.ride.infrastructure.persistence.RideStatusHistoryEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideStatusHistoryJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.StatusActorType;
import com.ridingplatform.ride.infrastructure.persistence.StatusSourceType;
import com.ridingplatform.config.ApplicationProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverAssignmentService {

    private final ApplicationProperties applicationProperties;
    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository;
    private final RideJpaRepository rideJpaRepository;
    private final DriverAssignmentAttemptJpaRepository driverAssignmentAttemptJpaRepository;
    private final DriverAvailabilityJpaRepository driverAvailabilityJpaRepository;
    private final DriverProfileJpaRepository driverProfileJpaRepository;
    private final RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository;
    private final DriverAvailabilityService driverAvailabilityService;
    private final DriverAssignmentScoreCalculator driverAssignmentScoreCalculator;
    private final DriverAssignmentCandidateCache driverAssignmentCandidateCache;
    private final RideEventPublisher rideEventPublisher;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public DriverAssignmentService(
            ApplicationProperties applicationProperties,
            SpringDataRideRequestJpaRepository rideRequestJpaRepository,
            RideJpaRepository rideJpaRepository,
            DriverAssignmentAttemptJpaRepository driverAssignmentAttemptJpaRepository,
            DriverAvailabilityJpaRepository driverAvailabilityJpaRepository,
            DriverProfileJpaRepository driverProfileJpaRepository,
            RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository,
            DriverAvailabilityService driverAvailabilityService,
            DriverAssignmentScoreCalculator driverAssignmentScoreCalculator,
            DriverAssignmentCandidateCache driverAssignmentCandidateCache,
            RideEventPublisher rideEventPublisher,
            IdGenerator idGenerator,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.applicationProperties = applicationProperties;
        this.rideRequestJpaRepository = rideRequestJpaRepository;
        this.rideJpaRepository = rideJpaRepository;
        this.driverAssignmentAttemptJpaRepository = driverAssignmentAttemptJpaRepository;
        this.driverAvailabilityJpaRepository = driverAvailabilityJpaRepository;
        this.driverProfileJpaRepository = driverProfileJpaRepository;
        this.rideStatusHistoryJpaRepository = rideStatusHistoryJpaRepository;
        this.driverAvailabilityService = driverAvailabilityService;
        this.driverAssignmentScoreCalculator = driverAssignmentScoreCalculator;
        this.driverAssignmentCandidateCache = driverAssignmentCandidateCache;
        this.rideEventPublisher = rideEventPublisher;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<DriverAssignmentResult> assignBestDriver(DriverAssignmentRequest request) {
        RideRequestEntity rideRequest = rideRequestJpaRepository.findByIdForUpdate(request.rideRequestId())
                .orElseThrow(() -> new RideNotFoundException("Ride request not found"));
        RideEntity ride = rideJpaRepository.findByBookingRequestIdForUpdate(request.rideRequestId())
                .orElseThrow(() -> new RideNotFoundException("Ride not found for ride request"));
        if (!isDispatchable(rideRequest, ride)) {
            return Optional.empty();
        }

        List<DriverAssignmentAttemptEntity> existingAttempts = driverAssignmentAttemptJpaRepository.findByRideRequestIdOrderByAttemptNoAsc(rideRequest.getId());
        int dispatchRound = nextDispatchRound(existingAttempts, request.trigger());
        if (dispatchRound > applicationProperties.dispatch().maxDispatchRounds()) {
            markDispatchFailed(rideRequest, ride, DriverAssignmentFailureReason.MAX_DISPATCH_ROUNDS_EXCEEDED, dispatchRound);
            return Optional.empty();
        }

        List<DriverAssignmentCandidateScore> rankedCandidates = rankCandidates(rideRequest, existingAttempts);
        driverAssignmentCandidateCache.put(rideRequest.getId(), rankedCandidates);
        if (rankedCandidates.isEmpty()) {
            markDispatchFailed(rideRequest, ride, DriverAssignmentFailureReason.NO_CANDIDATES_AVAILABLE, dispatchRound);
            return Optional.empty();
        }

        int nextAttemptNo = existingAttempts.isEmpty()
                ? 1
                : existingAttempts.get(existingAttempts.size() - 1).getAttemptNo() + 1;

        for (DriverAssignmentCandidateScore score : rankedCandidates) {
            DriverAssignmentAttemptEntity attempt = createAttempt(rideRequest, ride, dispatchRound, nextAttemptNo++, score);
            Optional<DriverAssignmentResult> assignment = tryAssignCandidate(rideRequest, ride, attempt, score);
            if (assignment.isPresent()) {
                return assignment;
            }
        }

        markDispatchFailed(rideRequest, ride, DriverAssignmentFailureReason.NO_CANDIDATES_AVAILABLE, dispatchRound);
        return Optional.empty();
    }

    @Transactional
    public Optional<UUID> expirePendingAssignment(UUID assignmentAttemptId) {
        DriverAssignmentAttemptEntity attempt = driverAssignmentAttemptJpaRepository.findByIdForUpdate(assignmentAttemptId)
                .orElseThrow(() -> new RideNotFoundException("Assignment attempt not found"));
        if (attempt.getAssignmentStatus() != DriverAssignmentAttemptStatus.PENDING_DRIVER_RESPONSE) {
            return Optional.empty();
        }
        RideRequestEntity rideRequest = rideRequestJpaRepository.findByIdForUpdate(attempt.getRideRequest().getId())
                .orElseThrow(() -> new RideNotFoundException("Ride request not found"));
        RideEntity ride = rideJpaRepository.findByBookingRequestIdForUpdate(rideRequest.getId())
                .orElseThrow(() -> new RideNotFoundException("Ride not found for ride request"));

        Instant now = Instant.now(clock);
        attempt.setAssignmentStatus(DriverAssignmentAttemptStatus.TIMED_OUT);
        attempt.setFailureReasonCode(DriverAssignmentFailureReason.DRIVER_RESPONSE_TIMEOUT);
        attempt.setRespondedAt(now);
        attempt.setResponseNote("Driver response timeout");
        driverAssignmentAttemptJpaRepository.save(attempt);

        if (attempt.getDriverProfile() != null) {
            releaseDriverReservation(attempt.getDriverProfile().getId(), ride.getId());
        }
        clearAssignment(rideRequest, ride, "Driver assignment timed out");
        publishAssignmentEvent(RideEventType.DRIVER_ASSIGNMENT_TIMED_OUT, attempt, now);
        return Optional.of(rideRequest.getId());
    }

    private Optional<DriverAssignmentResult> tryAssignCandidate(
            RideRequestEntity rideRequest,
            RideEntity ride,
            DriverAssignmentAttemptEntity attempt,
            DriverAssignmentCandidateScore score
    ) {
        Instant now = Instant.now(clock);
        Optional<DriverAvailabilityEntity> lockableAvailability = driverAvailabilityJpaRepository.findByDriverProfileIdForUpdate(score.driverProfileId());
        if (lockableAvailability.isEmpty()) {
            markAttemptFailure(attempt, DriverAssignmentFailureReason.DRIVER_NOT_AVAILABLE, "Driver availability row not found");
            return Optional.empty();
        }
        DriverAvailabilityEntity availability = lockableAvailability.get();
        if (!isAssignable(availability, rideRequest, score.candidate())) {
            markAttemptFailure(attempt, resolveFailure(availability, score.candidate()), "Candidate lost eligibility before assignment");
            return Optional.empty();
        }

        availability.setCurrentRideId(ride.getId());
        availability.setAvailabilityStatus(AvailabilityStatus.BUSY);
        availability.setAvailableSince(null);
        driverAvailabilityJpaRepository.save(availability);

        ride.setDriverProfile(availability.getDriverProfile());
        ride.setVehicle(availability.getDriverProfile().getCurrentVehicle());
        ride.setAssignedAt(now);
        ride.setLifecycleStatus(RideLifecycleStatus.DRIVER_ASSIGNED);
        rideJpaRepository.save(ride);

        transitionRideRequest(rideRequest, RideRequestStatusEntityType.DRIVER_ASSIGNED, "Driver assigned by dispatch engine");
        writeRideStatusHistory(ride, RideLifecycleStatus.SEARCHING_DRIVER, RideLifecycleStatus.DRIVER_ASSIGNED, "Driver assigned by dispatch engine");

        attempt.setAssignmentStatus(DriverAssignmentAttemptStatus.PENDING_DRIVER_RESPONSE);
        attempt.setOfferedAt(now);
        attempt.setExpiresAt(now.plusSeconds(applicationProperties.dispatch().assignmentTimeoutSeconds()));
        attempt.setAssignmentToken(idGenerator.nextId());
        driverAssignmentAttemptJpaRepository.save(attempt);

        publishAssignmentEvent(RideEventType.DRIVER_ASSIGNED, attempt, now);
        return Optional.of(new DriverAssignmentResult(
                rideRequest.getId(),
                ride.getId(),
                availability.getDriverProfile().getId(),
                availability.getDriverProfile().getCurrentVehicle() == null ? null : availability.getDriverProfile().getCurrentVehicle().getId(),
                true,
                attempt.getDispatchRound(),
                attempt.getAttemptNo(),
                attempt.getExpiresAt()
        ));
    }

    private List<DriverAssignmentCandidateScore> rankCandidates(
            RideRequestEntity rideRequest,
            List<DriverAssignmentAttemptEntity> existingAttempts
    ) {
        Set<UUID> excludedDriverIds = existingAttempts.stream()
                .map(DriverAssignmentAttemptEntity::getDriverProfile)
                .filter(java.util.Objects::nonNull)
                .map(driver -> driver.getId())
                .collect(Collectors.toSet());

        List<NearbyDriverResult> nearbyDrivers = driverAvailabilityService.searchNearby(new NearbyDriverSearchCommand(
                rideRequest.getOrigin().getY(),
                rideRequest.getOrigin().getX(),
                applicationProperties.dispatch().score().maxDistanceMeters(),
                applicationProperties.dispatch().candidateLimit(),
                AvailabilityStatus.AVAILABLE,
                rideRequest.getRequestedVehicleType(),
                true,
                null,
                true
        ));

        Map<UUID, NearbyDriverResult> distanceMap = nearbyDrivers.stream()
                .filter(driver -> !excludedDriverIds.contains(driver.driverProfileId()))
                .collect(Collectors.toMap(NearbyDriverResult::driverProfileId, driver -> driver, (left, right) -> left, LinkedHashMap::new));
        if (distanceMap.isEmpty()) {
            return List.of();
        }

        Map<UUID, DriverDispatchCandidate> candidateMap = driverProfileJpaRepository.findDispatchCandidates(distanceMap.keySet()).stream()
                .filter(candidate -> !excludedDriverIds.contains(candidate.driverProfileId()))
                .filter(candidate -> isVehicleCompatible(rideRequest, candidate))
                .collect(Collectors.toMap(DriverDispatchCandidate::driverProfileId, candidate -> candidate));

        return distanceMap.values().stream()
                .map(nearby -> candidateMap.get(nearby.driverProfileId()))
                .filter(java.util.Objects::nonNull)
                .map(candidate -> driverAssignmentScoreCalculator.score(candidate, distanceMap.get(candidate.driverProfileId()).distanceMeters()))
                .sorted(Comparator.comparingDouble(DriverAssignmentCandidateScore::totalScore).reversed())
                .limit(applicationProperties.dispatch().candidateLimit())
                .toList();
    }

    private DriverAssignmentAttemptEntity createAttempt(
            RideRequestEntity rideRequest,
            RideEntity ride,
            int dispatchRound,
            int attemptNo,
            DriverAssignmentCandidateScore score
    ) {
        DriverAssignmentAttemptEntity attempt = new DriverAssignmentAttemptEntity();
        attempt.setId(idGenerator.nextId());
        attempt.setRideRequest(rideRequest);
        attempt.setRide(ride);
        attempt.setDispatchRound(dispatchRound);
        attempt.setAttemptNo(attemptNo);
        attempt.setDriverProfile(driverProfileJpaRepository.getReferenceById(score.driverProfileId()));
        attempt.setVehicle(score.vehicleId() == null ? null : attempt.getDriverProfile().getCurrentVehicle());
        attempt.setAssignmentStatus(DriverAssignmentAttemptStatus.FAILED);
        attempt.setWeightedScore(BigDecimal.valueOf(score.totalScore()).setScale(6, RoundingMode.HALF_UP));
        attempt.setDistanceMeters(BigDecimal.valueOf(score.distanceMeters()).setScale(2, RoundingMode.HALF_UP));
        attempt.setEtaSeconds(score.etaSeconds());
        attempt.setScoreBreakdownJson(serializeScore(score));
        attempt.setOfferedAt(Instant.now(clock));
        return attempt;
    }

    private boolean isAssignable(
            DriverAvailabilityEntity availability,
            RideRequestEntity rideRequest,
            DriverDispatchCandidate candidate
    ) {
        if (availability.getOnlineStatus() != OnlineStatus.ONLINE) {
            return false;
        }
        if (availability.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            return false;
        }
        if (availability.getCurrentRideId() != null) {
            return false;
        }
        return isVehicleCompatible(rideRequest, candidate) && !candidate.riskBlocked();
    }

    private DriverAssignmentFailureReason resolveFailure(DriverAvailabilityEntity availability, DriverDispatchCandidate candidate) {
        if (candidate.riskBlocked()) {
            return DriverAssignmentFailureReason.RISK_BLOCKED;
        }
        if (availability.getOnlineStatus() != OnlineStatus.ONLINE) {
            return DriverAssignmentFailureReason.DRIVER_OFFLINE;
        }
        if (availability.getCurrentRideId() != null || availability.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            return DriverAssignmentFailureReason.RACE_LOST;
        }
        return DriverAssignmentFailureReason.VEHICLE_INCOMPATIBLE;
    }

    private void markAttemptFailure(DriverAssignmentAttemptEntity attempt, DriverAssignmentFailureReason reason, String note) {
        attempt.setAssignmentStatus(DriverAssignmentAttemptStatus.FAILED);
        attempt.setFailureReasonCode(reason);
        attempt.setResponseNote(note);
        attempt.setRespondedAt(Instant.now(clock));
        driverAssignmentAttemptJpaRepository.save(attempt);
        publishAssignmentEvent(RideEventType.DRIVER_ASSIGNMENT_FAILED, attempt, Instant.now(clock));
    }

    private void markDispatchFailed(
            RideRequestEntity rideRequest,
            RideEntity ride,
            DriverAssignmentFailureReason reason,
            int dispatchRound
    ) {
        transitionRideRequest(rideRequest, RideRequestStatusEntityType.FAILED, "Dispatch failed: " + reason.name());
        RideLifecycleStatus previous = ride.getLifecycleStatus();
        ride.setLifecycleStatus(RideLifecycleStatus.FAILED);
        rideJpaRepository.save(ride);
        writeRideStatusHistory(ride, previous, RideLifecycleStatus.FAILED, "Dispatch failed: " + reason.name());
        rideEventPublisher.publish(RideEventType.DRIVER_ASSIGNMENT_FAILED, rideRequest.getId(), new DriverAssignmentEventPayload(
                rideRequest.getId(),
                ride.getId(),
                null,
                null,
                DriverAssignmentAttemptStatus.FAILED.name(),
                reason.name(),
                0,
                0,
                0,
                dispatchRound,
                0,
                Instant.now(clock)
        ));
    }

    private void transitionRideRequest(
            RideRequestEntity rideRequest,
            RideRequestStatusEntityType targetStatus,
            String note
    ) {
        RideRequestStatusEntityType previous = rideRequest.getRequestStatus();
        rideRequest.setRequestStatus(targetStatus);
        rideRequestJpaRepository.save(rideRequest);

        RideStatusHistoryEntity history = new RideStatusHistoryEntity();
        history.setId(idGenerator.nextId());
        history.setRideRequest(rideRequest);
        history.setPreviousStatus(previous == null ? null : previous.name());
        history.setCurrentStatus(targetStatus.name());
        history.setSourceType(StatusSourceType.RIDE_REQUEST);
        history.setActorType(StatusActorType.SYSTEM);
        history.setNote(note);
        history.setChangedAt(Instant.now(clock));
        history.setCreatedAt(Instant.now(clock));
        rideStatusHistoryJpaRepository.save(history);
    }

    private void writeRideStatusHistory(
            RideEntity ride,
            RideLifecycleStatus previousStatus,
            RideLifecycleStatus currentStatus,
            String note
    ) {
        RideStatusHistoryEntity history = new RideStatusHistoryEntity();
        history.setId(idGenerator.nextId());
        history.setRide(ride);
        history.setPreviousStatus(previousStatus == null ? null : previousStatus.name());
        history.setCurrentStatus(currentStatus.name());
        history.setSourceType(StatusSourceType.RIDE);
        history.setActorType(StatusActorType.SYSTEM);
        history.setNote(note);
        history.setChangedAt(Instant.now(clock));
        history.setCreatedAt(Instant.now(clock));
        rideStatusHistoryJpaRepository.save(history);
    }

    private void clearAssignment(RideRequestEntity rideRequest, RideEntity ride, String note) {
        RideLifecycleStatus previousRideStatus = ride.getLifecycleStatus();
        ride.setDriverProfile(null);
        ride.setVehicle(null);
        ride.setAssignedAt(null);
        ride.setLifecycleStatus(RideLifecycleStatus.SEARCHING_DRIVER);
        rideJpaRepository.save(ride);
        writeRideStatusHistory(ride, previousRideStatus, RideLifecycleStatus.SEARCHING_DRIVER, note);
        transitionRideRequest(rideRequest, RideRequestStatusEntityType.SEARCHING_DRIVER, note);
        driverAssignmentCandidateCache.evict(rideRequest.getId());
    }

    private void releaseDriverReservation(UUID driverProfileId, UUID rideId) {
        driverAvailabilityJpaRepository.findByDriverProfileIdForUpdate(driverProfileId).ifPresent(availability -> {
            if (rideId.equals(availability.getCurrentRideId())) {
                availability.setCurrentRideId(null);
                availability.setAvailabilityStatus(availability.getOnlineStatus() == OnlineStatus.ONLINE
                        ? AvailabilityStatus.AVAILABLE
                        : AvailabilityStatus.OFFLINE);
                availability.setAvailableSince(Instant.now(clock));
                driverAvailabilityJpaRepository.save(availability);
            }
        });
    }

    private boolean isDispatchable(RideRequestEntity rideRequest, RideEntity ride) {
        return rideRequest.getRequestStatus() == RideRequestStatusEntityType.SEARCHING_DRIVER
                && ride.getLifecycleStatus() == RideLifecycleStatus.SEARCHING_DRIVER;
    }

    private boolean isVehicleCompatible(RideRequestEntity rideRequest, DriverDispatchCandidate candidate) {
        if (candidate.vehicleId() == null || candidate.vehicleType() == null) {
            return false;
        }
        if (candidate.seatCapacity() < rideRequest.getSeatCount()) {
            return false;
        }
        return rideRequest.getRequestedVehicleType() == null || rideRequest.getRequestedVehicleType() == candidate.vehicleType();
    }

    private int nextDispatchRound(List<DriverAssignmentAttemptEntity> existingAttempts, AssignmentTrigger trigger) {
        if (existingAttempts.isEmpty()) {
            return 1;
        }
        int latestRound = existingAttempts.get(existingAttempts.size() - 1).getDispatchRound();
        return trigger == AssignmentTrigger.BOOKING_REQUESTED ? latestRound : latestRound + 1;
    }

    private void publishAssignmentEvent(RideEventType eventType, DriverAssignmentAttemptEntity attempt, Instant occurredAt) {
        rideEventPublisher.publish(eventType, attempt.getRideRequest().getId(), new DriverAssignmentEventPayload(
                attempt.getRideRequest().getId(),
                attempt.getRide().getId(),
                attempt.getDriverProfile() == null ? null : attempt.getDriverProfile().getId(),
                attempt.getVehicle() == null ? null : attempt.getVehicle().getId(),
                attempt.getAssignmentStatus().name(),
                attempt.getFailureReasonCode() == null ? null : attempt.getFailureReasonCode().name(),
                attempt.getWeightedScore() == null ? 0 : attempt.getWeightedScore().doubleValue(),
                attempt.getDistanceMeters() == null ? 0 : attempt.getDistanceMeters().doubleValue(),
                attempt.getEtaSeconds() == null ? 0 : attempt.getEtaSeconds(),
                attempt.getDispatchRound(),
                attempt.getAttemptNo(),
                occurredAt
        ));
    }

    private String serializeScore(DriverAssignmentCandidateScore score) {
        Map<String, Object> payload = Map.of(
                "distanceComponent", score.distanceComponent(),
                "etaComponent", score.etaComponent(),
                "ratingComponent", score.ratingComponent(),
                "acceptanceComponent", score.acceptanceComponent(),
                "cancellationComponent", score.cancellationComponent(),
                "idleTimeComponent", score.idleTimeComponent(),
                "fraudPenaltyComponent", score.fraudPenaltyComponent(),
                "vehicleCompatibilityComponent", score.vehicleCompatibilityComponent(),
                "availabilityComponent", score.availabilityComponent()
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
