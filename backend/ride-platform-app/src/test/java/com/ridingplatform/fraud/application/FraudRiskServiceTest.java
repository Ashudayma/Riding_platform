package com.ridingplatform.fraud.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.driver.application.DriverRedisStateService;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileJpaRepository;
import com.ridingplatform.fraud.domain.FraudSignal;
import com.ridingplatform.fraud.domain.FraudSignalType;
import com.ridingplatform.fraud.domain.RiskAssessment;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudReviewActionJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSignalEventJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class FraudRiskServiceTest {

    private final FraudWindowCounterService fraudWindowCounterService = Mockito.mock(FraudWindowCounterService.class);
    private final FraudRiskProfileJpaRepository fraudRiskProfileJpaRepository = Mockito.mock(FraudRiskProfileJpaRepository.class);
    private final FraudFlagJpaRepository fraudFlagJpaRepository = Mockito.mock(FraudFlagJpaRepository.class);
    private final FraudSignalEventJpaRepository fraudSignalEventJpaRepository = Mockito.mock(FraudSignalEventJpaRepository.class);
    private final FraudReviewActionJpaRepository fraudReviewActionJpaRepository = Mockito.mock(FraudReviewActionJpaRepository.class);
    private final RiderProfileJpaRepository riderProfileJpaRepository = Mockito.mock(RiderProfileJpaRepository.class);
    private final DriverProfileJpaRepository driverProfileJpaRepository = Mockito.mock(DriverProfileJpaRepository.class);
    private final DriverAvailabilityJpaRepository driverAvailabilityJpaRepository = Mockito.mock(DriverAvailabilityJpaRepository.class);
    private final DriverRedisStateService driverRedisStateService = Mockito.mock(DriverRedisStateService.class);
    private final RiskScoringModelClient riskScoringModelClient = Mockito.mock(RiskScoringModelClient.class);
    private final FraudKafkaEventPublisher fraudKafkaEventPublisher = Mockito.mock(FraudKafkaEventPublisher.class);
    private final IdGenerator idGenerator = Mockito.mock(IdGenerator.class);
    private final EntityManager entityManager = Mockito.mock(EntityManager.class);

    private FraudRiskService fraudRiskService;

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties(
                "riding-platform",
                new ApplicationProperties.Dispatch(15, 10, 20, 100, 2, 3, 22,
                        new ApplicationProperties.Score(0.24, 0.22, 0.12, 0.12, 0.10, 0.08, 0.08, 0.02, 0.02, 1800, 1800, 5000)),
                new ApplicationProperties.SharedRide(12, 3, 3, 10, 30, 4000, 0.55d, 0.18d),
                new ApplicationProperties.Realtime(10000, 1000, 20, 90),
                new ApplicationProperties.Fraud(4, 3, 5, 4, 24, 6, 24, 12, 24, 120, 140, 35, 1500, 420, 25, 50, 80, 12)
        );
        fraudRiskService = new FraudRiskService(
                properties,
                List.of(new RepeatedCancellationFraudRule(), new FailedPaymentAttemptsFraudRule(), new GpsSpoofingFraudRule()),
                fraudWindowCounterService,
                fraudRiskProfileJpaRepository,
                fraudFlagJpaRepository,
                fraudSignalEventJpaRepository,
                fraudReviewActionJpaRepository,
                riderProfileJpaRepository,
                driverProfileJpaRepository,
                driverAvailabilityJpaRepository,
                driverRedisStateService,
                riskScoringModelClient,
                fraudKafkaEventPublisher,
                idGenerator,
                new ObjectMapper(),
                entityManager,
                Clock.fixed(Instant.parse("2026-04-18T12:00:00Z"), ZoneOffset.UTC)
        );
        when(idGenerator.nextId()).thenReturn(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                UUID.fromString("10000000-0000-0000-0000-000000000003")
        );
        when(fraudRiskProfileJpaRepository.save(any(FraudRiskProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudFlagJpaRepository.save(any(FraudFlagEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fraudSignalEventJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(riskScoringModelClient.score(any(), any())).thenReturn(Optional.empty());
        when(fraudFlagJpaRepository.existsBySubjectTypeAndSubjectIdAndRuleCodeAndFlagStatusInAndDeletedAtIsNull(any(), any(), any(), any())).thenReturn(false);
    }

    @Test
    void shouldCreateRiderRiskFlagAndFraudHoldForRepeatedCancellations() {
        UUID riderId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        when(fraudWindowCounterService.increment(any(), any())).thenReturn(4L);
        when(fraudRiskProfileJpaRepository.findBySubjectTypeAndSubjectIdForUpdate(FraudSubjectType.RIDER, riderId))
                .thenReturn(Optional.empty(), Optional.of(profile(FraudSubjectType.RIDER, riderId)));
        when(fraudFlagJpaRepository.findBySubjectTypeAndSubjectIdAndFlagStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of(flag(FraudSubjectType.RIDER, riderId, new BigDecimal("22.0000"), FraudFlagStatus.OPEN)));
        RiderProfileEntity rider = new RiderProfileEntity();
        rider.setId(riderId);
        when(riderProfileJpaRepository.findById(riderId)).thenReturn(Optional.of(rider));
        when(fraudRiskProfileJpaRepository.findBySubjectTypeAndSubjectId(FraudSubjectType.RIDER, riderId)).thenReturn(Optional.of(profile(FraudSubjectType.RIDER, riderId)));

        RiskAssessment assessment = fraudRiskService.processSignal(new FraudSignal(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                FraudSignalType.RIDE_CANCELLED,
                FraudSubjectType.RIDER,
                riderId,
                riderId,
                null,
                null,
                null,
                null,
                "riding-platform.ride.cancelled",
                riderId.toString(),
                Instant.parse("2026-04-18T12:00:00Z"),
                Map.of()
        ));

        assertThat(assessment.triggeredSignals()).contains("REPEATED_CANCELLATIONS");
        verify(riderProfileJpaRepository).save(any(RiderProfileEntity.class));
    }

    @Test
    void shouldBlockDriverWhenGpsSpoofingIsCritical() {
        UUID driverId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        when(fraudRiskProfileJpaRepository.findBySubjectTypeAndSubjectIdForUpdate(FraudSubjectType.DRIVER, driverId))
                .thenReturn(Optional.empty(), Optional.of(profile(FraudSubjectType.DRIVER, driverId)));
        when(fraudFlagJpaRepository.findBySubjectTypeAndSubjectIdAndFlagStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(List.of(flag(FraudSubjectType.DRIVER, driverId, new BigDecimal("45.0000"), FraudFlagStatus.CONFIRMED)));
        DriverProfileEntity driver = new DriverProfileEntity();
        driver.setId(driverId);
        driver.setRiskScore(BigDecimal.ZERO);
        when(driverProfileJpaRepository.findById(driverId)).thenReturn(Optional.of(driver));

        fraudRiskService.processSignal(new FraudSignal(
                UUID.fromString("20000000-0000-0000-0000-000000000002"),
                FraudSignalType.DRIVER_LOCATION_UPDATE,
                FraudSubjectType.DRIVER,
                driverId,
                null,
                driverId,
                null,
                null,
                null,
                "riding-platform.tracking.location-updated",
                driverId.toString(),
                Instant.parse("2026-04-18T12:00:00Z"),
                Map.of("speedKph", new BigDecimal("160"), "accuracyMeters", new BigDecimal("8"))
        ));

        verify(driverProfileJpaRepository).save(any(DriverProfileEntity.class));
        verify(driverRedisStateService).updateRiskBlocked(driverId, true);
    }

    private FraudRiskProfileEntity profile(FraudSubjectType subjectType, UUID subjectId) {
        FraudRiskProfileEntity entity = new FraudRiskProfileEntity();
        entity.setId(UUID.fromString("50000000-0000-0000-0000-000000000001"));
        entity.setSubjectType(subjectType);
        entity.setSubjectId(subjectId);
        entity.setAggregateScore(BigDecimal.ZERO);
        entity.setRiskLevel(com.ridingplatform.fraud.domain.RiskLevel.LOW);
        return entity;
    }

    private FraudFlagEntity flag(FraudSubjectType subjectType, UUID subjectId, BigDecimal score, FraudFlagStatus status) {
        FraudFlagEntity entity = new FraudFlagEntity();
        entity.setId(UUID.fromString("60000000-0000-0000-0000-000000000001"));
        entity.setSubjectType(subjectType);
        entity.setSubjectId(subjectId);
        entity.setRiskScore(score);
        entity.setFlagStatus(status);
        entity.setSeverity(com.ridingplatform.fraud.domain.RiskLevel.CRITICAL);
        return entity;
    }
}
