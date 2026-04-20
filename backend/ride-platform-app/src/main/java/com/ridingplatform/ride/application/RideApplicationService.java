package com.ridingplatform.ride.application;

import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.common.persistence.GeoFactory;
import com.ridingplatform.payment.infrastructure.persistence.PaymentMethodEntity;
import com.ridingplatform.payment.infrastructure.persistence.PaymentMethodJpaRepository;
import com.ridingplatform.payment.infrastructure.persistence.PaymentMethodStatus;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteJpaRepository;
import com.ridingplatform.ride.domain.RideType;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.CancellationActorType;
import com.ridingplatform.ride.infrastructure.persistence.RequestedRideType;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideLifecycleStatus;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import com.ridingplatform.ride.infrastructure.persistence.RideStatusHistoryEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideStatusHistoryJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideStopEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideStopJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.StatusActorType;
import com.ridingplatform.ride.infrastructure.persistence.StatusSourceType;
import com.ridingplatform.ride.infrastructure.persistence.StopStatus;
import com.ridingplatform.ride.infrastructure.persistence.StopType;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import com.ridingplatform.sharedride.application.SharedRideMatchingService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RideApplicationService {

    private static final Map<RideRequestStatusEntityType, List<RideRequestStatusEntityType>> ALLOWED_TRANSITIONS =
            new EnumMap<>(RideRequestStatusEntityType.class);

    static {
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.REQUESTED, List.of(RideRequestStatusEntityType.SEARCHING_DRIVER, RideRequestStatusEntityType.CANCELLED, RideRequestStatusEntityType.FAILED));
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.SEARCHING_DRIVER, List.of(RideRequestStatusEntityType.DRIVER_ASSIGNED, RideRequestStatusEntityType.CANCELLED, RideRequestStatusEntityType.FAILED));
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.DRIVER_ASSIGNED, List.of(RideRequestStatusEntityType.DRIVER_ARRIVING, RideRequestStatusEntityType.CANCELLED));
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.DRIVER_ARRIVING, List.of(RideRequestStatusEntityType.DRIVER_ARRIVED, RideRequestStatusEntityType.CANCELLED));
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.DRIVER_ARRIVED, List.of(RideRequestStatusEntityType.IN_PROGRESS, RideRequestStatusEntityType.CANCELLED));
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.IN_PROGRESS, List.of(RideRequestStatusEntityType.COMPLETED, RideRequestStatusEntityType.FAILED));
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.COMPLETED, List.of());
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.CANCELLED, List.of());
        ALLOWED_TRANSITIONS.put(RideRequestStatusEntityType.FAILED, List.of());
    }

    private final RiderProfileJpaRepository riderProfileJpaRepository;
    private final PaymentMethodJpaRepository paymentMethodJpaRepository;
    private final FareQuoteJpaRepository fareQuoteJpaRepository;
    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository;
    private final RideStopJpaRepository rideStopJpaRepository;
    private final RideJpaRepository rideJpaRepository;
    private final RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository;
    private final RidePricingService ridePricingService;
    private final RideEventPublisher rideEventPublisher;
    private final SharedRideMatchingService sharedRideMatchingService;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final StringRedisTemplate stringRedisTemplate;

    public RideApplicationService(
            RiderProfileJpaRepository riderProfileJpaRepository,
            PaymentMethodJpaRepository paymentMethodJpaRepository,
            FareQuoteJpaRepository fareQuoteJpaRepository,
            SpringDataRideRequestJpaRepository rideRequestJpaRepository,
            RideStopJpaRepository rideStopJpaRepository,
            RideJpaRepository rideJpaRepository,
            RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository,
            RidePricingService ridePricingService,
            RideEventPublisher rideEventPublisher,
            SharedRideMatchingService sharedRideMatchingService,
            IdGenerator idGenerator,
            Clock clock,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.riderProfileJpaRepository = riderProfileJpaRepository;
        this.paymentMethodJpaRepository = paymentMethodJpaRepository;
        this.fareQuoteJpaRepository = fareQuoteJpaRepository;
        this.rideRequestJpaRepository = rideRequestJpaRepository;
        this.rideStopJpaRepository = rideStopJpaRepository;
        this.rideJpaRepository = rideJpaRepository;
        this.rideStatusHistoryJpaRepository = rideStatusHistoryJpaRepository;
        this.ridePricingService = ridePricingService;
        this.rideEventPublisher = rideEventPublisher;
        this.sharedRideMatchingService = sharedRideMatchingService;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public FareEstimateResult estimateFare(FareEstimateCommand command) {
        validateCommand(command);
        FareEstimateResult result = ridePricingService.estimate(command);
        rideEventPublisher.publish(RideEventType.FARE_ESTIMATED, result.fareQuoteId(), result);
        return result;
    }

    @Transactional
    public RideBookingResult bookRide(BookRideCommand command) {
        validateCommand(new FareEstimateCommand(
                command.riderProfileId(),
                command.rideType(),
                command.seatCount(),
                command.requestedVehicleType(),
                command.pickupLatitude(),
                command.pickupLongitude(),
                command.pickupAddress(),
                command.dropLatitude(),
                command.dropLongitude(),
                command.dropAddress(),
                command.stops()
        ));
        RiderProfileEntity rider = riderProfileJpaRepository.findById(command.riderProfileId())
                .orElseThrow(() -> new RideValidationException("Rider profile not found"));
        PaymentMethodEntity paymentMethod = paymentMethodJpaRepository.findById(command.paymentMethodId())
                .filter(method -> method.getRiderProfile().getId().equals(command.riderProfileId()))
                .filter(method -> method.getPaymentMethodStatus() == PaymentMethodStatus.ACTIVE)
                .orElseThrow(() -> new RideValidationException("Active payment method not found for rider"));
        FareEstimateResult estimate = ridePricingService.estimate(new FareEstimateCommand(
                command.riderProfileId(),
                command.rideType(),
                command.seatCount(),
                command.requestedVehicleType(),
                command.pickupLatitude(),
                command.pickupLongitude(),
                command.pickupAddress(),
                command.dropLatitude(),
                command.dropLongitude(),
                command.dropAddress(),
                command.stops()
        ));
        FareQuoteEntity fareQuote = fareQuoteJpaRepository.findById(estimate.fareQuoteId())
                .orElseThrow(() -> new RideValidationException("Fare quote not found"));

        Instant now = Instant.now(clock);
        RideRequestEntity rideRequest = new RideRequestEntity();
        rideRequest.setId(idGenerator.nextId());
        rideRequest.setRiderProfile(rider);
        rideRequest.setRequestedRideType(mapRideType(command.rideType()));
        rideRequest.setRequestStatus(RideRequestStatusEntityType.REQUESTED);
        rideRequest.setSeatCount(command.seatCount());
        rideRequest.setRequestedVehicleType(command.requestedVehicleType());
        rideRequest.setFareQuote(fareQuote);
        rideRequest.setPaymentMethod(paymentMethod);
        rideRequest.setOrigin(GeoFactory.point(command.pickupLatitude(), command.pickupLongitude()));
        rideRequest.setDestination(GeoFactory.point(command.dropLatitude(), command.dropLongitude()));
        rideRequest.setOriginAddress(command.pickupAddress());
        rideRequest.setDestinationAddress(command.dropAddress());
        rideRequest.setRequestedAt(now);
        rideRequest.setExpiresAt(now.plus(5, ChronoUnit.MINUTES));
        rideRequest.setNotes(command.notes());
        rideRequestJpaRepository.save(rideRequest);

        createStops(rideRequest, rider, command, now);
        transitionRequestStatus(rideRequest, RideRequestStatusEntityType.SEARCHING_DRIVER, StatusActorType.SYSTEM, "Dispatch search initiated");
        RideEntity ride = createShadowRide(rideRequest, rider, fareQuote, now);

        RideBookingResult result = toBookingResult(rideRequest, ride, estimate.totalAmount(), fareQuote.getCurrencyCode());
        rideEventPublisher.publish(RideEventType.BOOKING_REQUESTED, rideRequest.getId(), result);
        if (command.rideType() == RideType.SHARED) {
            sharedRideMatchingService.matchOrCreateGroup(rideRequest.getId());
        }
        stringRedisTemplate.delete(historyCacheKey(command.riderProfileId()));
        return result;
    }

    @Transactional
    public RideBookingResult cancelRide(CancelRideCommand command) {
        RideRequestEntity rideRequest = rideRequestJpaRepository.findById(command.rideRequestId())
                .orElseThrow(() -> new RideNotFoundException("Ride request not found"));
        if (!rideRequest.getRiderProfile().getId().equals(command.riderProfileId())) {
            throw new RideValidationException("Ride request does not belong to rider");
        }
        if (!List.of(
                RideRequestStatusEntityType.REQUESTED,
                RideRequestStatusEntityType.SEARCHING_DRIVER,
                RideRequestStatusEntityType.DRIVER_ASSIGNED,
                RideRequestStatusEntityType.DRIVER_ARRIVING,
                RideRequestStatusEntityType.DRIVER_ARRIVED
        ).contains(rideRequest.getRequestStatus())) {
            throw new RideStateTransitionException("Ride can no longer be cancelled in its current status");
        }

        rideRequest.setCancelReason(command.cancelReason());
        rideRequest.setCancelledAt(Instant.now(clock));
        transitionRequestStatus(rideRequest, RideRequestStatusEntityType.CANCELLED, StatusActorType.RIDER, command.cancelReason());

        rideJpaRepository.findByBookingRequestId(rideRequest.getId()).ifPresent(ride -> {
            ride.setLifecycleStatus(RideLifecycleStatus.CANCELLED);
            ride.setCancelledAt(Instant.now(clock));
            ride.setCancellationActorType(CancellationActorType.RIDER);
            ride.setCancellationReason(command.cancelReason());
            rideJpaRepository.save(ride);
        });

        FareQuoteEntity quote = rideRequest.getFareQuote();
        RideBookingResult result = toBookingResult(
                rideRequest,
                rideJpaRepository.findByBookingRequestId(rideRequest.getId()).orElse(null),
                quote != null ? quote.getTotalAmount() : null,
                quote != null ? quote.getCurrencyCode() : "INR"
        );
        rideEventPublisher.publish(RideEventType.BOOKING_CANCELLED, rideRequest.getId(), result);
        stringRedisTemplate.delete(historyCacheKey(command.riderProfileId()));
        return result;
    }

    @Transactional(readOnly = true)
    public RideBookingResult getRideDetails(UUID rideRequestId, UUID riderProfileId) {
        RideRequestEntity rideRequest = rideRequestJpaRepository.findById(rideRequestId)
                .orElseThrow(() -> new RideNotFoundException("Ride request not found"));
        if (!rideRequest.getRiderProfile().getId().equals(riderProfileId)) {
            throw new RideValidationException("Ride request does not belong to rider");
        }
        RideEntity ride = rideJpaRepository.findByBookingRequestId(rideRequestId).orElse(null);
        FareQuoteEntity quote = rideRequest.getFareQuote();
        return toBookingResult(rideRequest, ride, quote != null ? quote.getTotalAmount() : null, quote != null ? quote.getCurrencyCode() : "INR");
    }

    @Transactional(readOnly = true)
    public List<RideHistoryItemResult> getRideHistory(UUID riderProfileId) {
        String cacheKey = historyCacheKey(riderProfileId);
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            List<RideHistoryItemResult> items = new ArrayList<>();
            for (String row : cached.split("\n")) {
                String[] parts = row.split("\\|");
                items.add(new RideHistoryItemResult(
                        UUID.fromString(parts[0]),
                        "null".equals(parts[1]) ? null : UUID.fromString(parts[1]),
                        RideType.valueOf(parts[2]),
                        RideBookingStatus.valueOf(parts[3]),
                        parts[4],
                        parts[5],
                        new java.math.BigDecimal(parts[6]),
                        parts[7],
                        Instant.parse(parts[8])
                ));
            }
            return items;
        }
        List<RideHistoryItemResult> results = rideRequestJpaRepository.findTop50ByRiderProfileIdAndDeletedAtIsNullOrderByRequestedAtDesc(riderProfileId)
                .stream()
                .map(rideRequest -> {
                    RideEntity ride = rideJpaRepository.findByBookingRequestId(rideRequest.getId()).orElse(null);
                    FareQuoteEntity quote = rideRequest.getFareQuote();
                    return new RideHistoryItemResult(
                            rideRequest.getId(),
                            ride == null ? null : ride.getId(),
                            mapRideType(rideRequest.getRequestedRideType()),
                            mapStatus(rideRequest.getRequestStatus()),
                            rideRequest.getOriginAddress(),
                            rideRequest.getDestinationAddress(),
                            quote != null ? quote.getTotalAmount() : java.math.BigDecimal.ZERO,
                            quote != null ? quote.getCurrencyCode() : "INR",
                            rideRequest.getRequestedAt()
                    );
                })
                .toList();
        stringRedisTemplate.opsForValue().set(cacheKey, serializeHistory(results), java.time.Duration.ofMinutes(2));
        return results;
    }

    private void createStops(RideRequestEntity rideRequest, RiderProfileEntity rider, BookRideCommand command, Instant now) {
        List<RideStopEntity> stops = new ArrayList<>();
        int sequence = 1;
        stops.add(stop(rideRequest, null, rider, StopType.PICKUP, sequence++, command.pickupLatitude(), command.pickupLongitude(), command.pickupAddress(), null, now));
        if (command.stops() != null) {
            for (RideStopCommand intermediate : command.stops()) {
                if (intermediate.stopType() == StopTypeCommand.WAYPOINT) {
                    stops.add(stop(rideRequest, null, rider, StopType.WAYPOINT, sequence++, intermediate.latitude(), intermediate.longitude(), intermediate.address(), intermediate.locality(), now));
                }
            }
        }
        stops.add(stop(rideRequest, null, rider, StopType.DROPOFF, sequence, command.dropLatitude(), command.dropLongitude(), command.dropAddress(), null, now));
        rideStopJpaRepository.saveAll(stops);
    }

    private RideStopEntity stop(
            RideRequestEntity rideRequest,
            RideEntity ride,
            RiderProfileEntity rider,
            StopType stopType,
            int requestSequence,
            double latitude,
            double longitude,
            String address,
            String locality,
            Instant now
    ) {
        RideStopEntity stop = new RideStopEntity();
        stop.setId(idGenerator.nextId());
        stop.setRideRequest(rideRequest);
        stop.setRide(ride);
        stop.setRiderProfile(rider);
        stop.setStopType(stopType);
        stop.setStopStatus(StopStatus.PLANNED);
        stop.setRequestSequenceNo(requestSequence);
        stop.setStopPoint(GeoFactory.point(latitude, longitude));
        stop.setAddressLine(address);
        stop.setLocality(locality);
        stop.setPassengerCount((short) 1);
        stop.setCreatedAt(now);
        stop.setUpdatedAt(now);
        return stop;
    }

    private RideEntity createShadowRide(RideRequestEntity request, RiderProfileEntity rider, FareQuoteEntity fareQuote, Instant now) {
        RideEntity ride = new RideEntity();
        ride.setId(idGenerator.nextId());
        ride.setPublicRideCode("RIDE-" + now.toEpochMilli());
        ride.setBookingRequest(request);
        ride.setBookingRiderProfile(rider);
        ride.setRideType(request.getRequestedRideType());
        ride.setLifecycleStatus(RideLifecycleStatus.SEARCHING_DRIVER);
        ride.setFinalFareQuote(fareQuote);
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);
        rideJpaRepository.save(ride);
        return ride;
    }

    private void transitionRequestStatus(
            RideRequestEntity rideRequest,
            RideRequestStatusEntityType targetStatus,
            StatusActorType actorType,
            String note
    ) {
        RideRequestStatusEntityType current = rideRequest.getRequestStatus();
        if (current != null && !ALLOWED_TRANSITIONS.getOrDefault(current, List.of()).contains(targetStatus)) {
            throw new RideStateTransitionException("Invalid ride status transition from " + current + " to " + targetStatus);
        }
        rideRequest.setRequestStatus(targetStatus);
        rideRequestJpaRepository.save(rideRequest);

        RideStatusHistoryEntity history = new RideStatusHistoryEntity();
        history.setId(idGenerator.nextId());
        history.setRideRequest(rideRequest);
        history.setPreviousStatus(current == null ? null : current.name());
        history.setCurrentStatus(targetStatus.name());
        history.setSourceType(StatusSourceType.RIDE_REQUEST);
        history.setActorType(actorType);
        history.setNote(note);
        history.setChangedAt(Instant.now(clock));
        history.setCreatedAt(Instant.now(clock));
        rideStatusHistoryJpaRepository.save(history);
    }

    private RideBookingResult toBookingResult(RideRequestEntity rideRequest, RideEntity ride, java.math.BigDecimal amount, String currencyCode) {
        List<RideStopResult> stops = rideStopJpaRepository.findByRideRequestIdOrderByRequestSequenceNoAsc(rideRequest.getId())
                .stream()
                .map(stop -> new RideStopResult(
                        stop.getId(),
                        stop.getStopType().name(),
                        stop.getRequestSequenceNo(),
                        stop.getRideSequenceNo(),
                        stop.getAddressLine()
                ))
                .toList();
        return new RideBookingResult(
                rideRequest.getId(),
                ride == null ? null : ride.getId(),
                rideRequest.getRiderProfile().getId(),
                mapRideType(rideRequest.getRequestedRideType()),
                mapStatus(rideRequest.getRequestStatus()),
                rideRequest.getFareQuote() == null ? null : rideRequest.getFareQuote().getId(),
                amount,
                currencyCode,
                rideRequest.getRequestedAt(),
                stops
        );
    }

    private RequestedRideType mapRideType(RideType rideType) {
        return rideType == RideType.SHARED ? RequestedRideType.SHARED : RequestedRideType.STANDARD;
    }

    private RideType mapRideType(RequestedRideType rideType) {
        return rideType == RequestedRideType.SHARED ? RideType.SHARED : RideType.STANDARD;
    }

    private RideBookingStatus mapStatus(RideRequestStatusEntityType requestStatus) {
        return RideBookingStatus.valueOf(requestStatus.name());
    }

    private void validateCommand(FareEstimateCommand command) {
        if (command.pickupAddress() == null || command.pickupAddress().isBlank() || command.dropAddress() == null || command.dropAddress().isBlank()) {
            throw new RideValidationException("Pickup and drop addresses are required");
        }
        if (command.seatCount() < 1 || command.seatCount() > 4) {
            throw new RideValidationException("Seat count must be between 1 and 4");
        }
        if (command.rideType() == RideType.STANDARD && command.seatCount() > 1 && command.requestedVehicleType() == null) {
            throw new RideValidationException("Vehicle type must be provided for multi-seat standard rides");
        }
    }

    private String historyCacheKey(UUID riderProfileId) {
        return "ride-history:" + riderProfileId;
    }

    private String serializeHistory(List<RideHistoryItemResult> items) {
        StringBuilder builder = new StringBuilder();
        for (RideHistoryItemResult item : items) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(item.rideRequestId()).append('|')
                    .append(item.rideId()).append('|')
                    .append(item.rideType()).append('|')
                    .append(item.status()).append('|')
                    .append(item.pickupAddress()).append('|')
                    .append(item.dropAddress()).append('|')
                    .append(item.amount()).append('|')
                    .append(item.currencyCode()).append('|')
                    .append(item.requestedAt());
        }
        return builder.toString();
    }
}
