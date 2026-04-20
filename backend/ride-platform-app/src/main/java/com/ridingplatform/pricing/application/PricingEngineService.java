package com.ridingplatform.pricing.application;

import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.pricing.infrastructure.persistence.FareBreakdownItemEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareBreakdownItemJpaRepository;
import com.ridingplatform.pricing.infrastructure.persistence.FareLineType;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteJpaRepository;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteStatus;
import com.ridingplatform.pricing.infrastructure.persistence.PricingRuleSetEntity;
import com.ridingplatform.ride.application.RideStopCommand;
import com.ridingplatform.ride.domain.RideType;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingEngineService {

    private final PricingRuleCacheService pricingRuleCacheService;
    private final SurgePricingService surgePricingService;
    private final FareQuoteJpaRepository fareQuoteJpaRepository;
    private final FareBreakdownItemJpaRepository fareBreakdownItemJpaRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final EntityManager entityManager;

    public PricingEngineService(
            PricingRuleCacheService pricingRuleCacheService,
            SurgePricingService surgePricingService,
            FareQuoteJpaRepository fareQuoteJpaRepository,
            FareBreakdownItemJpaRepository fareBreakdownItemJpaRepository,
            IdGenerator idGenerator,
            Clock clock,
            EntityManager entityManager
    ) {
        this.pricingRuleCacheService = pricingRuleCacheService;
        this.surgePricingService = surgePricingService;
        this.fareQuoteJpaRepository = fareQuoteJpaRepository;
        this.fareBreakdownItemJpaRepository = fareBreakdownItemJpaRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.entityManager = entityManager;
    }

    @Transactional
    public FareQuoteResult estimate(EstimateFareCommand command) {
        PricingRequestContext context = new PricingRequestContext(
                command.cityCode(),
                command.zoneCode(),
                command.rideType(),
                command.requestedVehicleType()
        );
        PricingRuleSnapshot rule = pricingRuleCacheService.resolve(context);
        BigDecimal surgeMultiplier = surgePricingService.surgeMultiplier(context).min(rule.surgeCapMultiplier());
        int distanceMeters = estimateDistanceMeters(command.pickupLatitude(), command.pickupLongitude(), command.dropLatitude(), command.dropLongitude(), command.stops());
        int durationSeconds = Math.max(420, distanceMeters / 7);
        BigDecimal baseFare = rule.baseFare();
        BigDecimal distanceFare = metersToKm(distanceMeters).multiply(rule.perKmRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal durationFare = secondsToMinutes(durationSeconds).multiply(rule.perMinuteRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal stopCharge = BigDecimal.valueOf(command.stops() == null ? 0 : command.stops().size()).multiply(rule.perStopCharge()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal preSurgeSubtotal = baseFare.add(distanceFare).add(durationFare).add(rule.bookingFee()).add(stopCharge);
        BigDecimal surgeAmount = preSurgeSubtotal.multiply(surgeMultiplier.subtract(BigDecimal.ONE)).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal poolingDiscount = command.rideType() == RideType.SHARED
                ? preSurgeSubtotal.multiply(rule.sharedDiscountFactor()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal subtotal = preSurgeSubtotal.add(surgeAmount).subtract(poolingDiscount).max(rule.minimumFare());
        BigDecimal taxAmount = subtotal.multiply(rule.taxPercentage()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(10, ChronoUnit.MINUTES);

        FareQuoteEntity entity = new FareQuoteEntity();
        entity.setId(idGenerator.nextId());
        entity.setPricingStatus(FareQuoteStatus.ESTIMATED);
        entity.setCurrencyCode(rule.currencyCode());
        entity.setBaseFare(baseFare);
        entity.setDistanceFare(distanceFare);
        entity.setDurationFare(durationFare);
        entity.setSurgeMultiplier(surgeMultiplier);
        entity.setSurgeAmount(surgeAmount);
        entity.setBookingFee(rule.bookingFee());
        entity.setWaitingCharge(BigDecimal.ZERO);
        entity.setCancellationCharge(BigDecimal.ZERO);
        entity.setTaxAmount(taxAmount);
        entity.setTollAmount(BigDecimal.ZERO);
        entity.setDiscountAmount(BigDecimal.ZERO);
        entity.setPoolingDiscountAmount(poolingDiscount);
        entity.setRoundingAdjustment(BigDecimal.ZERO);
        entity.setSubtotalAmount(subtotal);
        entity.setTotalAmount(total);
        entity.setPricingStrategyCode(command.rideType().name() + "_RULESET_V" + rule.pricingVersion());
        entity.setPricingRuleSet(rule.pricingRuleSetId() == null ? null : entityManager.getReference(PricingRuleSetEntity.class, rule.pricingRuleSetId()));
        entity.setCityCode(rule.cityCode());
        entity.setZoneCode(rule.zoneCode());
        entity.setPricingVersion(rule.pricingVersion());
        entity.setQuotedDistanceMeters(distanceMeters);
        entity.setQuotedDurationSeconds(durationSeconds);
        entity.setExpiresAt(expiresAt);
        fareQuoteJpaRepository.save(entity);

        persistBreakdown(entity, baseFare, distanceFare, durationFare, stopCharge, surgeAmount, rule.bookingFee(), poolingDiscount, BigDecimal.ZERO, BigDecimal.ZERO, taxAmount);
        return toResult(entity, command.rideType(), distanceMeters, durationSeconds);
    }

    @Transactional
    public FareQuoteResult finalizeFare(FinalizeFareCommand command, RideType rideType) {
        FareQuoteEntity entity = fareQuoteJpaRepository.findById(command.fareQuoteId())
                .orElseThrow(() -> new IllegalArgumentException("Fare quote not found"));
        PricingRuleSnapshot rule = new PricingRuleSnapshot(
                entity.getPricingRuleSet() == null ? null : entity.getPricingRuleSet().getId(),
                entity.getCityCode(),
                entity.getZoneCode(),
                entity.getPricingVersion() == null ? 1 : entity.getPricingVersion(),
                entity.getCurrencyCode(),
                entity.getBaseFare(),
                entity.getSubtotalAmount(),
                entity.getBookingFee(),
                rate(entity.getDistanceFare(), entity.getQuotedDistanceMeters()),
                rate(entity.getDurationFare(), entity.getQuotedDurationSeconds() == null ? null : entity.getQuotedDurationSeconds() / 60),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                entity.getPoolingDiscountAmount().compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("0.15") : BigDecimal.ZERO,
                entity.getTaxAmount().compareTo(BigDecimal.ZERO) > 0 && entity.getSubtotalAmount().compareTo(BigDecimal.ZERO) > 0
                        ? entity.getTaxAmount().divide(entity.getSubtotalAmount(), 4, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                entity.getSurgeMultiplier(),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        BigDecimal distanceFare = metersToKm(command.actualDistanceMeters()).multiply(rule.perKmRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal durationFare = secondsToMinutes(command.actualDurationSeconds()).multiply(rule.perMinuteRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal waitingCharge = secondsToMinutes(command.waitingDurationSeconds()).multiply(rule.waitingChargePerMinute()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cancellationCharge = command.cancelled()
                ? rule.cancellationBaseCharge().add(metersToKm(command.cancelledAfterDistanceMeters()).multiply(rule.cancellationPerKmCharge())).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal poolingDiscount = rideType == RideType.SHARED
                ? entity.getBaseFare().add(distanceFare).add(durationFare).multiply(rule.sharedDiscountFactor()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal subtotal = entity.getBaseFare()
                .add(distanceFare)
                .add(durationFare)
                .add(entity.getBookingFee())
                .add(waitingCharge)
                .add(cancellationCharge)
                .add(entity.getSurgeAmount())
                .subtract(poolingDiscount)
                .max(entity.getBaseFare());
        BigDecimal taxAmount = subtotal.multiply(rule.taxPercentage()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        entity.setPricingStatus(FareQuoteStatus.FINALIZED);
        entity.setDistanceFare(distanceFare);
        entity.setDurationFare(durationFare);
        entity.setWaitingCharge(waitingCharge);
        entity.setCancellationCharge(cancellationCharge);
        entity.setPoolingDiscountAmount(poolingDiscount);
        entity.setSubtotalAmount(subtotal);
        entity.setTaxAmount(taxAmount);
        entity.setTotalAmount(total);
        entity.setFinalizedDistanceMeters(command.actualDistanceMeters());
        entity.setFinalizedDurationSeconds(command.actualDurationSeconds());
        entity.setFinalizedAt(Instant.now(clock));
        fareQuoteJpaRepository.save(entity);

        persistBreakdown(entity, entity.getBaseFare(), distanceFare, durationFare, BigDecimal.ZERO, entity.getSurgeAmount(), entity.getBookingFee(), poolingDiscount, waitingCharge, cancellationCharge, taxAmount);
        return toResult(entity, rideType, command.actualDistanceMeters(), command.actualDurationSeconds());
    }

    private void persistBreakdown(
            FareQuoteEntity quoteEntity,
            BigDecimal baseFare,
            BigDecimal distanceFare,
            BigDecimal durationFare,
            BigDecimal stopCharge,
            BigDecimal surgeAmount,
            BigDecimal bookingFee,
            BigDecimal poolingDiscount,
            BigDecimal waitingCharge,
            BigDecimal cancellationCharge,
            BigDecimal taxAmount
    ) {
        List<FareBreakdownItemEntity> items = new ArrayList<>();
        items.add(item(quoteEntity, FareLineType.BASE, "BASE_FARE", "Base fare", baseFare, 1));
        items.add(item(quoteEntity, FareLineType.DISTANCE, "DISTANCE_FARE", "Distance fare", distanceFare, 2));
        items.add(item(quoteEntity, FareLineType.DURATION, "TIME_FARE", "Duration fare", durationFare, 3));
        if (stopCharge.compareTo(BigDecimal.ZERO) > 0) {
            items.add(item(quoteEntity, FareLineType.DURATION, "STOP_CHARGE", "Additional stop charge", stopCharge, 4));
        }
        if (surgeAmount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(item(quoteEntity, FareLineType.SURGE, "SURGE", "Demand surge", surgeAmount, 5));
        }
        items.add(item(quoteEntity, FareLineType.BOOKING_FEE, "BOOKING_FEE", "Booking fee", bookingFee, 6));
        if (waitingCharge.compareTo(BigDecimal.ZERO) > 0) {
            items.add(item(quoteEntity, FareLineType.DURATION, "WAITING_CHARGE", "Waiting charge", waitingCharge, 7));
        }
        if (cancellationCharge.compareTo(BigDecimal.ZERO) > 0) {
            items.add(item(quoteEntity, FareLineType.BOOKING_FEE, "CANCELLATION_CHARGE", "Cancellation charge", cancellationCharge, 8));
        }
        if (poolingDiscount.compareTo(BigDecimal.ZERO) > 0) {
            items.add(item(quoteEntity, FareLineType.POOLING_DISCOUNT, "POOL_DISCOUNT", "Shared ride discount", poolingDiscount.negate(), 9));
        }
        items.add(item(quoteEntity, FareLineType.TAX, "GST", "Tax", taxAmount, 10));
        fareBreakdownItemJpaRepository.saveAll(items);
    }

    private FareBreakdownItemEntity item(FareQuoteEntity quote, FareLineType type, String code, String description, BigDecimal amount, int sortOrder) {
        FareBreakdownItemEntity item = new FareBreakdownItemEntity();
        item.setId(idGenerator.nextId());
        item.setFareQuote(quote);
        item.setLineType(type);
        item.setLineCode(code);
        item.setDescription(description);
        item.setAmount(amount);
        item.setSortOrder(sortOrder);
        item.setCreatedAt(Instant.now(clock));
        return item;
    }

    private FareQuoteResult toResult(FareQuoteEntity entity, RideType rideType, int distanceMeters, int durationSeconds) {
        return new FareQuoteResult(
                entity.getId(),
                rideType,
                entity.getCityCode(),
                entity.getZoneCode(),
                entity.getPricingVersion(),
                entity.getBaseFare(),
                entity.getDistanceFare(),
                entity.getDurationFare(),
                entity.getSurgeMultiplier(),
                entity.getSurgeAmount(),
                entity.getBookingFee(),
                entity.getWaitingCharge(),
                entity.getCancellationCharge(),
                entity.getTaxAmount(),
                entity.getDiscountAmount(),
                entity.getPoolingDiscountAmount(),
                entity.getTotalAmount(),
                entity.getCurrencyCode(),
                distanceMeters,
                durationSeconds,
                entity.getExpiresAt(),
                entity.getFinalizedAt()
        );
    }

    private BigDecimal metersToKm(int meters) {
        return BigDecimal.valueOf(meters).divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal secondsToMinutes(int seconds) {
        return BigDecimal.valueOf(seconds).divide(new BigDecimal("60"), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(BigDecimal amount, Integer divisor) {
        if (amount == null || divisor == null || divisor <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(BigDecimal.valueOf(divisor), 4, RoundingMode.HALF_UP);
    }

    private int estimateDistanceMeters(double pickupLatitude, double pickupLongitude, double dropLatitude, double dropLongitude, List<RideStopCommand> stops) {
        double latDiff = dropLatitude - pickupLatitude;
        double lonDiff = dropLongitude - pickupLongitude;
        double baseDistance = Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * 111_000d;
        double additionalStopDistance = stops == null ? 0 : stops.size() * 1_500d;
        return Math.max(1_000, (int) Math.round(baseDistance + additionalStopDistance));
    }
}
