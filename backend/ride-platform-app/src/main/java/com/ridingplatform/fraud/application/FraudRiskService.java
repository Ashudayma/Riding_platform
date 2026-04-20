package com.ridingplatform.fraud.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.driver.application.DriverRedisStateService;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileJpaRepository;
import com.ridingplatform.fraud.domain.FraudProfileSummary;
import com.ridingplatform.fraud.domain.FraudReviewActionType;
import com.ridingplatform.fraud.domain.FraudRuleMatch;
import com.ridingplatform.fraud.domain.FraudSignal;
import com.ridingplatform.fraud.domain.RiskAssessment;
import com.ridingplatform.fraud.domain.RiskLevel;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudReviewActionEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudReviewActionJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSignalEventEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSignalEventJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudRiskService {

    private static final List<FraudFlagStatus> ACTIVE_FLAG_STATUSES = List.of(
            FraudFlagStatus.OPEN,
            FraudFlagStatus.UNDER_REVIEW,
            FraudFlagStatus.CONFIRMED
    );

    private final ApplicationProperties applicationProperties;
    private final List<FraudRule> fraudRules;
    private final FraudWindowCounterService fraudWindowCounterService;
    private final FraudRiskProfileJpaRepository fraudRiskProfileJpaRepository;
    private final FraudFlagJpaRepository fraudFlagJpaRepository;
    private final FraudSignalEventJpaRepository fraudSignalEventJpaRepository;
    private final FraudReviewActionJpaRepository fraudReviewActionJpaRepository;
    private final RiderProfileJpaRepository riderProfileJpaRepository;
    private final DriverProfileJpaRepository driverProfileJpaRepository;
    private final DriverAvailabilityJpaRepository driverAvailabilityJpaRepository;
    private final DriverRedisStateService driverRedisStateService;
    private final RiskScoringModelClient riskScoringModelClient;
    private final FraudKafkaEventPublisher fraudKafkaEventPublisher;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final Clock clock;

    public FraudRiskService(
            ApplicationProperties applicationProperties,
            List<FraudRule> fraudRules,
            FraudWindowCounterService fraudWindowCounterService,
            FraudRiskProfileJpaRepository fraudRiskProfileJpaRepository,
            FraudFlagJpaRepository fraudFlagJpaRepository,
            FraudSignalEventJpaRepository fraudSignalEventJpaRepository,
            FraudReviewActionJpaRepository fraudReviewActionJpaRepository,
            RiderProfileJpaRepository riderProfileJpaRepository,
            DriverProfileJpaRepository driverProfileJpaRepository,
            DriverAvailabilityJpaRepository driverAvailabilityJpaRepository,
            DriverRedisStateService driverRedisStateService,
            RiskScoringModelClient riskScoringModelClient,
            FraudKafkaEventPublisher fraudKafkaEventPublisher,
            IdGenerator idGenerator,
            ObjectMapper objectMapper,
            EntityManager entityManager,
            Clock clock
    ) {
        this.applicationProperties = applicationProperties;
        this.fraudRules = fraudRules;
        this.fraudWindowCounterService = fraudWindowCounterService;
        this.fraudRiskProfileJpaRepository = fraudRiskProfileJpaRepository;
        this.fraudFlagJpaRepository = fraudFlagJpaRepository;
        this.fraudSignalEventJpaRepository = fraudSignalEventJpaRepository;
        this.fraudReviewActionJpaRepository = fraudReviewActionJpaRepository;
        this.riderProfileJpaRepository = riderProfileJpaRepository;
        this.driverProfileJpaRepository = driverProfileJpaRepository;
        this.driverAvailabilityJpaRepository = driverAvailabilityJpaRepository;
        this.driverRedisStateService = driverRedisStateService;
        this.riskScoringModelClient = riskScoringModelClient;
        this.fraudKafkaEventPublisher = fraudKafkaEventPublisher;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public RiskAssessment processSignal(FraudSignal signal) {
        FraudRiskProfileEntity profile = fraudRiskProfileJpaRepository.findBySubjectTypeAndSubjectIdForUpdate(signal.subjectType(), signal.subjectId())
                .orElseGet(() -> createProfile(signal.subjectType(), signal.subjectId(), signal.occurredAt()));
        List<FraudRuleMatch> matches = evaluate(signal, profile);
        persistSignal(signal, matches);
        createFlags(signal, matches);
        FraudProfileSummary summary = refreshProfile(profile.getSubjectType(), profile.getSubjectId(), signal.occurredAt());
        fraudKafkaEventPublisher.publishProfileUpdated(signal.subjectId(), summary);
        return new RiskAssessment(
                summary.profileId(),
                summary.subjectType().name(),
                summary.subjectId(),
                summary.aggregateScore(),
                summary.riskLevel(),
                matches.stream().map(FraudRuleMatch::ruleCode).toList(),
                signal.occurredAt()
        );
    }

    @Transactional(readOnly = true)
    public FraudProfileSummary getProfile(FraudSubjectType subjectType, UUID subjectId) {
        FraudRiskProfileEntity entity = fraudRiskProfileJpaRepository.findBySubjectTypeAndSubjectId(subjectType, subjectId)
                .orElseThrow(() -> new FraudNotFoundException("Fraud risk profile not found"));
        return toSummary(entity);
    }

    @Transactional(readOnly = true)
    public List<FraudFlagEntity> openFlags() {
        return fraudFlagJpaRepository.findByFlagStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(List.of(
                FraudFlagStatus.OPEN,
                FraudFlagStatus.UNDER_REVIEW
        ));
    }

    @Transactional(readOnly = true)
    public List<FraudFlagEntity> findFlags(FraudSubjectType subjectType, UUID subjectId) {
        return fraudFlagJpaRepository.findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(subjectType, subjectId);
    }

    @Transactional(readOnly = true)
    public List<FraudSignalEventEntity> recentSignals(FraudSubjectType subjectType, UUID subjectId) {
        return fraudSignalEventJpaRepository.findTop20BySubjectTypeAndSubjectIdAndDeletedAtIsNullOrderByOccurredAtDesc(subjectType, subjectId);
    }

    @Transactional
    public FraudProfileSummary reviewFlag(
            UUID flagId,
            FraudFlagStatus targetStatus,
            String note,
            Boolean manualBlock,
            UUID actorUserProfileId
    ) {
        FraudFlagEntity flag = fraudFlagJpaRepository.findById(flagId)
                .orElseThrow(() -> new FraudNotFoundException("Fraud flag not found"));
        if (EnumSet.of(FraudFlagStatus.DISMISSED, FraudFlagStatus.RESOLVED).contains(flag.getFlagStatus())
                && targetStatus == FraudFlagStatus.UNDER_REVIEW) {
            throw new FraudReviewException("Closed flags cannot be moved back to under review");
        }
        flag.setFlagStatus(targetStatus);
        if (EnumSet.of(FraudFlagStatus.CONFIRMED, FraudFlagStatus.DISMISSED, FraudFlagStatus.RESOLVED).contains(targetStatus)) {
            flag.setResolvedAt(Instant.now(clock));
            if (actorUserProfileId != null) {
                flag.setResolvedByUserProfile(entityManager.getReference(UserProfileEntity.class, actorUserProfileId));
            }
        }
        fraudFlagJpaRepository.save(flag);

        FraudRiskProfileEntity profile = fraudRiskProfileJpaRepository.findBySubjectTypeAndSubjectIdForUpdate(flag.getSubjectType(), flag.getSubjectId())
                .orElseGet(() -> createProfile(flag.getSubjectType(), flag.getSubjectId(), Instant.now(clock)));
        if (manualBlock != null) {
            profile.setManualBlocked(manualBlock);
            profile.setBlockedReason(manualBlock ? "Manual fraud review block" : null);
            fraudRiskProfileJpaRepository.save(profile);
        }

        FraudReviewActionEntity action = new FraudReviewActionEntity();
        action.setId(idGenerator.nextId());
        action.setFraudFlag(flag);
        if (actorUserProfileId != null) {
            action.setActorUserProfile(entityManager.getReference(UserProfileEntity.class, actorUserProfileId));
        }
        action.setActionType(mapAction(targetStatus, manualBlock));
        action.setNote(note);
        action.setOccurredAt(Instant.now(clock));
        fraudReviewActionJpaRepository.save(action);

        return refreshProfile(flag.getSubjectType(), flag.getSubjectId(), Instant.now(clock));
    }

    private List<FraudRuleMatch> evaluate(FraudSignal signal, FraudRiskProfileEntity profile) {
        FraudRuleContext context = new FraudRuleContext(
                signal,
                profile,
                fraudWindowCounterService,
                applicationProperties.fraud(),
                clock
        );
        List<FraudRuleMatch> matches = new ArrayList<>();
        for (FraudRule rule : fraudRules) {
            if (rule.supports().contains(signal.signalType())) {
                matches.addAll(rule.evaluate(context));
            }
        }
        riskScoringModelClient.score(signal, profile).ifPresent(model -> matches.add(new FraudRuleMatch(
                "ML_" + model.modelName(),
                model.level(),
                model.score(),
                "ML risk recommendation",
                "The configured ML risk model recommended additional review for this signal.",
                model.blockRecommended(),
                model.evidence()
        )));
        return matches;
    }

    private FraudRiskProfileEntity createProfile(FraudSubjectType subjectType, UUID subjectId, Instant now) {
        FraudRiskProfileEntity entity = new FraudRiskProfileEntity();
        entity.setId(idGenerator.nextId());
        entity.setSubjectType(subjectType);
        entity.setSubjectId(subjectId);
        entity.setAggregateScore(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP));
        entity.setRiskLevel(RiskLevel.LOW);
        entity.setActiveFlagCount(0);
        entity.setLastSignalAt(now);
        entity.setLastAssessedAt(now);
        return fraudRiskProfileJpaRepository.save(entity);
    }

    private void createFlags(FraudSignal signal, List<FraudRuleMatch> matches) {
        for (FraudRuleMatch match : matches) {
            boolean exists = fraudFlagJpaRepository.existsBySubjectTypeAndSubjectIdAndRuleCodeAndFlagStatusInAndDeletedAtIsNull(
                    signal.subjectType(),
                    signal.subjectId(),
                    match.ruleCode(),
                    ACTIVE_FLAG_STATUSES
            );
            if (exists) {
                continue;
            }
            FraudFlagEntity entity = new FraudFlagEntity();
            entity.setId(idGenerator.nextId());
            entity.setSubjectType(signal.subjectType());
            entity.setSubjectId(signal.subjectId());
            entity.setSeverity(match.severity());
            entity.setFlagStatus(FraudFlagStatus.OPEN);
            entity.setRuleCode(match.ruleCode());
            entity.setRiskScore(match.score());
            entity.setTitle(match.title());
            entity.setDescription(match.description());
            entity.setEvidenceJson(writeJson(match.evidence()));
            fraudFlagJpaRepository.save(entity);
            fraudKafkaEventPublisher.publishFlagRaised(entity.getId(), Map.of(
                    "subjectType", entity.getSubjectType().name(),
                    "subjectId", entity.getSubjectId(),
                    "ruleCode", entity.getRuleCode(),
                    "severity", entity.getSeverity().name(),
                    "riskScore", entity.getRiskScore()
            ));
        }
    }

    private void persistSignal(FraudSignal signal, List<FraudRuleMatch> matches) {
        FraudSignalEventEntity entity = new FraudSignalEventEntity();
        entity.setId(signal.signalId());
        entity.setSignalType(signal.signalType().name());
        entity.setSubjectType(signal.subjectType());
        entity.setSubjectId(signal.subjectId());
        if (signal.riderProfileId() != null) {
            entity.setRiderProfile(entityManager.getReference(RiderProfileEntity.class, signal.riderProfileId()));
        }
        if (signal.driverProfileId() != null) {
            entity.setDriverProfile(entityManager.getReference(DriverProfileEntity.class, signal.driverProfileId()));
        }
        if (signal.rideRequestId() != null) {
            entity.setRideRequest(entityManager.getReference(com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity.class, signal.rideRequestId()));
        }
        if (signal.rideId() != null) {
            entity.setRide(entityManager.getReference(com.ridingplatform.ride.infrastructure.persistence.RideEntity.class, signal.rideId()));
        }
        if (signal.paymentTransactionId() != null) {
            entity.setPaymentTransaction(entityManager.getReference(com.ridingplatform.payment.infrastructure.persistence.PaymentTransactionEntity.class, signal.paymentTransactionId()));
        }
        entity.setSourceTopic(signal.sourceTopic());
        entity.setEventKey(signal.eventKey());
        entity.setAttributesJson(writeJson(signal.attributes()));
        entity.setTriggeredRulesJson(writeJson(matches.stream().map(FraudRuleMatch::ruleCode).toList()));
        entity.setOccurredAt(signal.occurredAt());
        fraudSignalEventJpaRepository.save(entity);
    }

    private FraudProfileSummary refreshProfile(FraudSubjectType subjectType, UUID subjectId, Instant assessedAt) {
        FraudRiskProfileEntity profile = fraudRiskProfileJpaRepository.findBySubjectTypeAndSubjectIdForUpdate(subjectType, subjectId)
                .orElseThrow(() -> new FraudNotFoundException("Fraud risk profile not found"));
        List<FraudFlagEntity> activeFlags = fraudFlagJpaRepository.findBySubjectTypeAndSubjectIdAndFlagStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(
                subjectType,
                subjectId,
                ACTIVE_FLAG_STATUSES
        );
        BigDecimal aggregateScore = activeFlags.stream()
                .map(FraudFlagEntity::getRiskScore)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(new BigDecimal("100.0000"))
                .setScale(4, RoundingMode.HALF_UP);
        RiskLevel riskLevel = riskLevel(aggregateScore.intValue());
        boolean derivedBlocked = riskLevel == RiskLevel.CRITICAL
                || activeFlags.stream().anyMatch(flag -> flag.getSeverity() == RiskLevel.CRITICAL || flag.getFlagStatus() == FraudFlagStatus.CONFIRMED);
        profile.setAggregateScore(aggregateScore);
        profile.setRiskLevel(riskLevel);
        profile.setActiveFlagCount(activeFlags.size());
        profile.setDerivedBlocked(derivedBlocked);
        profile.setBlocked(profile.isManualBlocked() || derivedBlocked);
        profile.setLastAssessedAt(assessedAt);
        if (profile.getLastSignalAt() == null || assessedAt.isAfter(profile.getLastSignalAt())) {
            profile.setLastSignalAt(assessedAt);
        }
        fraudRiskProfileJpaRepository.save(profile);
        syncSubjectState(profile);
        return toSummary(profile);
    }

    private void syncSubjectState(FraudRiskProfileEntity profile) {
        switch (profile.getSubjectType()) {
            case RIDER -> riderProfileJpaRepository.findById(profile.getSubjectId()).ifPresent(rider -> {
                rider.setFraudHold(profile.isBlocked());
                riderProfileJpaRepository.save(rider);
            });
            case DRIVER -> driverProfileJpaRepository.findById(profile.getSubjectId()).ifPresent(driver -> {
                driver.setRiskScore(profile.getAggregateScore().setScale(2, RoundingMode.HALF_UP));
                driver.setFraudBlocked(profile.isBlocked());
                driverProfileJpaRepository.save(driver);
                driverRedisStateService.updateRiskBlocked(driver.getId(), profile.isBlocked());
                driverAvailabilityJpaRepository.findByDriverProfileId(driver.getId()).ifPresent(availability -> {
                    double latitude = availability.getLastLocation() == null ? 0d : availability.getLastLocation().getY();
                    double longitude = availability.getLastLocation() == null ? 0d : availability.getLastLocation().getX();
                    driverRedisStateService.updateState(
                            availability,
                            driver.getCurrentVehicle() == null ? null : driver.getCurrentVehicle().getVehicleType(),
                            latitude,
                            longitude,
                            profile.isBlocked()
                    );
                });
            });
            default -> {
            }
        }
    }

    private FraudReviewActionType mapAction(FraudFlagStatus status, Boolean manualBlock) {
        if (manualBlock != null) {
            return manualBlock ? FraudReviewActionType.MANUAL_BLOCK : FraudReviewActionType.MANUAL_UNBLOCK;
        }
        return switch (status) {
            case UNDER_REVIEW -> FraudReviewActionType.UNDER_REVIEW;
            case CONFIRMED -> FraudReviewActionType.CONFIRM;
            case DISMISSED -> FraudReviewActionType.DISMISS;
            case RESOLVED -> FraudReviewActionType.RESOLVE;
            case OPEN -> FraudReviewActionType.NOTE;
        };
    }

    private RiskLevel riskLevel(int score) {
        if (score >= applicationProperties.fraud().criticalRiskThreshold()) {
            return RiskLevel.CRITICAL;
        }
        if (score >= applicationProperties.fraud().highRiskThreshold()) {
            return RiskLevel.HIGH;
        }
        if (score >= applicationProperties.fraud().mediumRiskThreshold()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private FraudProfileSummary toSummary(FraudRiskProfileEntity entity) {
        return new FraudProfileSummary(
                entity.getId(),
                entity.getSubjectType(),
                entity.getSubjectId(),
                entity.getAggregateScore(),
                entity.getRiskLevel(),
                entity.getActiveFlagCount(),
                entity.isDerivedBlocked(),
                entity.isManualBlocked(),
                entity.isBlocked(),
                entity.getBlockedReason(),
                entity.getLastSignalAt(),
                entity.getLastAssessedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
