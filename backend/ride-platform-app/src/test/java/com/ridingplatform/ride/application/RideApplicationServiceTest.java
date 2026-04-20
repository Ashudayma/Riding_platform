package com.ridingplatform.ride.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.payment.infrastructure.persistence.PaymentMethodEntity;
import com.ridingplatform.payment.infrastructure.persistence.PaymentMethodJpaRepository;
import com.ridingplatform.payment.infrastructure.persistence.PaymentMethodStatus;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteJpaRepository;
import com.ridingplatform.ride.domain.RideType;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideStatusHistoryJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideStopJpaRepository;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import com.ridingplatform.sharedride.application.SharedRideMatchingService;
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
import org.springframework.data.redis.core.StringRedisTemplate;

class RideApplicationServiceTest {

    private final RiderProfileJpaRepository riderProfileJpaRepository = Mockito.mock(RiderProfileJpaRepository.class);
    private final PaymentMethodJpaRepository paymentMethodJpaRepository = Mockito.mock(PaymentMethodJpaRepository.class);
    private final FareQuoteJpaRepository fareQuoteJpaRepository = Mockito.mock(FareQuoteJpaRepository.class);
    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository = Mockito.mock(SpringDataRideRequestJpaRepository.class);
    private final RideStopJpaRepository rideStopJpaRepository = Mockito.mock(RideStopJpaRepository.class);
    private final RideJpaRepository rideJpaRepository = Mockito.mock(RideJpaRepository.class);
    private final RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository = Mockito.mock(RideStatusHistoryJpaRepository.class);
    private final RidePricingService ridePricingService = Mockito.mock(RidePricingService.class);
    private final RideEventPublisher rideEventPublisher = Mockito.mock(RideEventPublisher.class);
    private final SharedRideMatchingService sharedRideMatchingService = Mockito.mock(SharedRideMatchingService.class);
    private final IdGenerator idGenerator = Mockito.mock(IdGenerator.class);
    private final StringRedisTemplate stringRedisTemplate = Mockito.mock(StringRedisTemplate.class);

    private RideApplicationService rideApplicationService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-04-18T10:00:00Z"), ZoneOffset.UTC);
        rideApplicationService = new RideApplicationService(
                riderProfileJpaRepository,
                paymentMethodJpaRepository,
                fareQuoteJpaRepository,
                rideRequestJpaRepository,
                rideStopJpaRepository,
                rideJpaRepository,
                rideStatusHistoryJpaRepository,
                ridePricingService,
                rideEventPublisher,
                sharedRideMatchingService,
                idGenerator,
                clock,
                stringRedisTemplate
        );

        when(idGenerator.nextId()).thenReturn(
                UUID.fromString("10000000-0000-0000-0000-000000000010"),
                UUID.fromString("10000000-0000-0000-0000-000000000011"),
                UUID.fromString("10000000-0000-0000-0000-000000000012"),
                UUID.fromString("10000000-0000-0000-0000-000000000013"),
                UUID.fromString("10000000-0000-0000-0000-000000000014"),
                UUID.fromString("10000000-0000-0000-0000-000000000015"),
                UUID.fromString("10000000-0000-0000-0000-000000000016")
        );
        when(rideRequestJpaRepository.save(any(RideRequestEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideJpaRepository.save(any(RideEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideStopJpaRepository.findByRideRequestIdOrderByRequestSequenceNoAsc(any())).thenReturn(List.of());
        when(rideJpaRepository.findByBookingRequestId(any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldBookRideAndPublishLifecycleEvent() {
        UUID riderProfileId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        UUID paymentMethodId = UUID.fromString("70000000-0000-0000-0000-000000000001");
        UUID fareQuoteId = UUID.fromString("80000000-0000-0000-0000-000000000001");

        RiderProfileEntity rider = new RiderProfileEntity();
        rider.setId(riderProfileId);
        PaymentMethodEntity paymentMethod = new PaymentMethodEntity();
        paymentMethod.setId(paymentMethodId);
        paymentMethod.setPaymentMethodStatus(PaymentMethodStatus.ACTIVE);
        paymentMethod.setRiderProfile(rider);
        FareQuoteEntity quote = new FareQuoteEntity();
        quote.setId(fareQuoteId);
        quote.setTotalAmount(new BigDecimal("250.00"));
        quote.setCurrencyCode("INR");

        when(riderProfileJpaRepository.findById(riderProfileId)).thenReturn(Optional.of(rider));
        when(paymentMethodJpaRepository.findById(paymentMethodId)).thenReturn(Optional.of(paymentMethod));
        when(ridePricingService.estimate(any())).thenReturn(new FareEstimateResult(
                fareQuoteId, RideType.STANDARD, new BigDecimal("70"), new BigDecimal("90"),
                new BigDecimal("30"), new BigDecimal("1.1"), new BigDecimal("15"), new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("250"), "INR", 12000, 1800, Instant.parse("2026-04-18T10:10:00Z")
        ));
        when(fareQuoteJpaRepository.findById(fareQuoteId)).thenReturn(Optional.of(quote));

        RideBookingResult result = rideApplicationService.bookRide(new BookRideCommand(
                riderProfileId,
                RideType.STANDARD,
                (short) 1,
                VehicleType.SEDAN,
                paymentMethodId,
                28.6139,
                77.2090,
                "Connaught Place",
                28.4595,
                77.0266,
                "Cyber Hub",
                List.of(),
                "No luggage"
        ));

        assertThat(result.rideRequestId()).isNotNull();
        assertThat(result.status()).isEqualTo(RideBookingStatus.SEARCHING_DRIVER);
        verify(rideEventPublisher).publish(any(), any(), any());
    }

    @Test
    void shouldRejectCancellationWhenRideAlreadyCompleted() {
        RideRequestEntity rideRequest = new RideRequestEntity();
        rideRequest.setId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
        RiderProfileEntity rider = new RiderProfileEntity();
        rider.setId(UUID.fromString("30000000-0000-0000-0000-000000000001"));
        rideRequest.setRiderProfile(rider);
        rideRequest.setRequestStatus(com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType.COMPLETED);
        when(rideRequestJpaRepository.findById(rideRequest.getId())).thenReturn(Optional.of(rideRequest));

        assertThatThrownBy(() -> rideApplicationService.cancelRide(new CancelRideCommand(
                rideRequest.getId(),
                rider.getId(),
                "Change of plans"
        ))).isInstanceOf(RideStateTransitionException.class);
    }
}
