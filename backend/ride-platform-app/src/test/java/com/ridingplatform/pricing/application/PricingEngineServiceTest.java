package com.ridingplatform.pricing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.pricing.infrastructure.persistence.FareBreakdownItemJpaRepository;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteJpaRepository;
import com.ridingplatform.ride.application.RideStopCommand;
import com.ridingplatform.ride.application.StopTypeCommand;
import com.ridingplatform.ride.domain.RideType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PricingEngineServiceTest {

    private final PricingRuleCacheService pricingRuleCacheService = Mockito.mock(PricingRuleCacheService.class);
    private final SurgePricingService surgePricingService = Mockito.mock(SurgePricingService.class);
    private final FareQuoteJpaRepository fareQuoteJpaRepository = Mockito.mock(FareQuoteJpaRepository.class);
    private final FareBreakdownItemJpaRepository fareBreakdownItemJpaRepository = Mockito.mock(FareBreakdownItemJpaRepository.class);
    private final IdGenerator idGenerator = Mockito.mock(IdGenerator.class);
    private final EntityManager entityManager = Mockito.mock(EntityManager.class);

    private PricingEngineService pricingEngineService;

    @BeforeEach
    void setUp() {
        pricingEngineService = new PricingEngineService(
                pricingRuleCacheService,
                surgePricingService,
                fareQuoteJpaRepository,
                fareBreakdownItemJpaRepository,
                idGenerator,
                Clock.fixed(Instant.parse("2026-04-18T12:00:00Z"), ZoneOffset.UTC),
                entityManager
        );
        when(idGenerator.nextId()).thenReturn(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("21111111-1111-1111-1111-111111111111"),
                UUID.fromString("31111111-1111-1111-1111-111111111111"),
                UUID.fromString("41111111-1111-1111-1111-111111111111"),
                UUID.fromString("51111111-1111-1111-1111-111111111111"),
                UUID.fromString("61111111-1111-1111-1111-111111111111"),
                UUID.fromString("71111111-1111-1111-1111-111111111111"),
                UUID.fromString("81111111-1111-1111-1111-111111111111"),
                UUID.fromString("91111111-1111-1111-1111-111111111111"),
                UUID.fromString("a1111111-1111-1111-1111-111111111111"),
                UUID.fromString("b1111111-1111-1111-1111-111111111111")
        );
        when(fareQuoteJpaRepository.save(any(FareQuoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldEstimateFareUsingVersionedPricingRule() {
        when(pricingRuleCacheService.resolve(any())).thenReturn(new PricingRuleSnapshot(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                "DELHI_NCR",
                "DEFAULT",
                2,
                "INR",
                new BigDecimal("70.00"),
                new BigDecimal("90.00"),
                new BigDecimal("15.00"),
                new BigDecimal("12.0000"),
                new BigDecimal("0.9000"),
                new BigDecimal("10.00"),
                new BigDecimal("1.5000"),
                new BigDecimal("40.00"),
                new BigDecimal("4.0000"),
                new BigDecimal("0.1800"),
                new BigDecimal("0.0500"),
                new BigDecimal("2.0000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));
        when(surgePricingService.surgeMultiplier(any())).thenReturn(new BigDecimal("1.3000"));

        FareQuoteResult result = pricingEngineService.estimate(new EstimateFareCommand(
                UUID.randomUUID(),
                RideType.SHARED,
                (short) 1,
                VehicleType.SEDAN,
                "DELHI_NCR",
                "DEFAULT",
                28.6139,
                77.2090,
                "Pickup",
                28.4595,
                77.0266,
                "Drop",
                List.of(new RideStopCommand(StopTypeCommand.WAYPOINT, 28.5700, 77.1800, "Waypoint", "Delhi"))
        ));

        assertThat(result.pricingVersion()).isEqualTo(2);
        assertThat(result.surgeMultiplier()).isEqualTo(new BigDecimal("1.3000"));
        assertThat(result.poolingDiscountAmount()).isPositive();
        assertThat(result.totalAmount()).isPositive();
    }

    @Test
    void shouldFinalizeFareWithWaitingAndCancellationCharges() {
        FareQuoteEntity entity = new FareQuoteEntity();
        entity.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        entity.setCurrencyCode("INR");
        entity.setBaseFare(new BigDecimal("70.00"));
        entity.setDistanceFare(new BigDecimal("90.00"));
        entity.setDurationFare(new BigDecimal("20.00"));
        entity.setBookingFee(new BigDecimal("15.00"));
        entity.setSurgeMultiplier(new BigDecimal("1.2000"));
        entity.setSurgeAmount(new BigDecimal("18.00"));
        entity.setDiscountAmount(BigDecimal.ZERO);
        entity.setPoolingDiscountAmount(new BigDecimal("25.00"));
        entity.setSubtotalAmount(new BigDecimal("188.00"));
        entity.setTaxAmount(new BigDecimal("9.40"));
        entity.setTotalAmount(new BigDecimal("197.40"));
        entity.setQuotedDistanceMeters(12000);
        entity.setQuotedDurationSeconds(1800);
        entity.setPricingVersion(2);
        entity.setCityCode("DELHI_NCR");
        entity.setZoneCode("DEFAULT");

        when(fareQuoteJpaRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        FareQuoteResult result = pricingEngineService.finalizeFare(
                new FinalizeFareCommand(entity.getId(), 15000, 2400, 300, true, 2000),
                RideType.SHARED
        );

        assertThat(result.waitingCharge()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.cancellationCharge()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(result.finalizedAt()).isNotNull();
    }
}
