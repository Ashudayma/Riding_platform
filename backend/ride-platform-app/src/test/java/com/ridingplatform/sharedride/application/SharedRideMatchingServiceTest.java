package com.ridingplatform.sharedride.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteJpaRepository;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RequestedRideType;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import com.ridingplatform.ride.infrastructure.persistence.RideStopEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideStopJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.StopType;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideCandidateJpaRepository;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupEntity;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupJpaRepository;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupStatus;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideRouteStopJpaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.mockito.Mockito;

class SharedRideMatchingServiceTest {

    private final SharedRideGroupJpaRepository sharedRideGroupJpaRepository = Mockito.mock(SharedRideGroupJpaRepository.class);
    private final SharedRideCandidateJpaRepository sharedRideCandidateJpaRepository = Mockito.mock(SharedRideCandidateJpaRepository.class);
    private final SharedRideRouteStopJpaRepository sharedRideRouteStopJpaRepository = Mockito.mock(SharedRideRouteStopJpaRepository.class);
    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository = Mockito.mock(SpringDataRideRequestJpaRepository.class);
    private final RideJpaRepository rideJpaRepository = Mockito.mock(RideJpaRepository.class);
    private final RideStopJpaRepository rideStopJpaRepository = Mockito.mock(RideStopJpaRepository.class);
    private final FareQuoteJpaRepository fareQuoteJpaRepository = Mockito.mock(FareQuoteJpaRepository.class);
    private final PoolRouteEvaluator poolRouteEvaluator = Mockito.mock(PoolRouteEvaluator.class);
    private final IdGenerator idGenerator = Mockito.mock(IdGenerator.class);

    private SharedRideMatchingService sharedRideMatchingService;
    private RideRequestEntity incomingRequest;
    private RideEntity incomingRide;

    @BeforeEach
    void setUp() {
        ApplicationProperties properties = new ApplicationProperties(
                "riding-platform",
                new ApplicationProperties.Dispatch(
                        15, 10, 20, 100, 2, 3, 22,
                        new ApplicationProperties.Score(0.24, 0.22, 0.12, 0.12, 0.10, 0.08, 0.08, 0.02, 0.02, 1800, 1800, 5000)
                ),
                new ApplicationProperties.SharedRide(12, 3, 3, 10, 30, 4000, 0.55d, 0.18d),
                new ApplicationProperties.Realtime(10000, 1000, 20, 90),
                new ApplicationProperties.Fraud(4, 3, 5, 4, 24, 6, 24, 12, 24, 120, 140, 35, 1500, 420, 25, 50, 80, 12)
        );
        sharedRideMatchingService = new SharedRideMatchingService(
                properties,
                sharedRideGroupJpaRepository,
                sharedRideCandidateJpaRepository,
                sharedRideRouteStopJpaRepository,
                rideRequestJpaRepository,
                rideJpaRepository,
                rideStopJpaRepository,
                fareQuoteJpaRepository,
                poolRouteEvaluator,
                idGenerator,
                Clock.fixed(Instant.parse("2026-04-18T12:00:00Z"), ZoneOffset.UTC),
                new ObjectMapper()
        );
        when(sharedRideGroupJpaRepository.save(any(SharedRideGroupEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sharedRideCandidateJpaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sharedRideRouteStopJpaRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(rideJpaRepository.save(any(RideEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fareQuoteJpaRepository.save(any(FareQuoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idGenerator.nextId()).thenReturn(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                UUID.fromString("10000000-0000-0000-0000-000000000003"),
                UUID.fromString("10000000-0000-0000-0000-000000000004"),
                UUID.fromString("10000000-0000-0000-0000-000000000005"),
                UUID.fromString("10000000-0000-0000-0000-000000000006"),
                UUID.fromString("10000000-0000-0000-0000-000000000007"),
                UUID.fromString("10000000-0000-0000-0000-000000000008"),
                UUID.fromString("10000000-0000-0000-0000-000000000009")
        );

        incomingRequest = rideRequest("90000000-0000-0000-0000-000000000001", "30000000-0000-0000-0000-000000000001", 28.6139, 77.2090, 28.6200, 77.2200);
        incomingRide = new RideEntity();
        incomingRide.setId(UUID.fromString("93000000-0000-0000-0000-000000000001"));
        incomingRide.setBookingRequest(incomingRequest);
    }

    @Test
    void shouldCreateStandaloneGroupWhenNoPoolingCandidateExists() {
        when(rideRequestJpaRepository.findByIdForUpdate(incomingRequest.getId())).thenReturn(Optional.of(incomingRequest));
        when(rideJpaRepository.findByBookingRequestIdForUpdate(incomingRequest.getId())).thenReturn(Optional.of(incomingRide));
        when(sharedRideGroupJpaRepository.findOpenGroups(any())).thenReturn(List.of());

        PoolMatchDecision decision = sharedRideMatchingService.matchOrCreateGroup(incomingRequest.getId());

        assertThat(decision.matched()).isFalse();
        assertThat(decision.rejectionReason()).isEqualTo("NO_POOLING_CANDIDATE");
        verify(sharedRideGroupJpaRepository).save(any(SharedRideGroupEntity.class));
    }

    @Test
    void shouldMatchIncomingRideIntoExistingGroupWhenRouteIsCompatible() {
        RideRequestEntity anchorRequest = rideRequest("90000000-0000-0000-0000-000000000010", "30000000-0000-0000-0000-000000000010", 28.6140, 77.2100, 28.6210, 77.2230);
        RideEntity anchorRide = new RideEntity();
        anchorRide.setId(UUID.fromString("93000000-0000-0000-0000-000000000010"));
        anchorRide.setBookingRequest(anchorRequest);
        SharedRideGroupEntity group = new SharedRideGroupEntity();
        group.setId(UUID.fromString("94000000-0000-0000-0000-000000000001"));
        group.setGroupStatus(SharedRideGroupStatus.FORMING);
        group.setAnchorRideRequest(anchorRequest);
        group.setMaxSeatCapacity((short) 4);
        group.setOccupiedSeatCount((short) 1);
        group.setPoolingSavingsAmount(BigDecimal.ZERO);

        PoolRoutePlan plan = new PoolRoutePlan(
                "PA->PB->DA->DB",
                List.of(
                        poolStop(anchorRequest, StopType.PICKUP, RouteStopRole.PICKUP_EXISTING, 28.6140, 77.2100),
                        poolStop(incomingRequest, StopType.PICKUP, RouteStopRole.PICKUP_NEW, 28.6139, 77.2090),
                        poolStop(anchorRequest, StopType.DROPOFF, RouteStopRole.DROPOFF_EXISTING, 28.6210, 77.2230),
                        poolStop(incomingRequest, StopType.DROPOFF, RouteStopRole.DROPOFF_NEW, 28.6200, 77.2200)
                ),
                2200,
                540,
                60,
                70,
                90,
                80,
                2,
                18,
                16
        );

        when(rideRequestJpaRepository.findByIdForUpdate(incomingRequest.getId())).thenReturn(Optional.of(incomingRequest));
        when(rideJpaRepository.findByBookingRequestIdForUpdate(incomingRequest.getId())).thenReturn(Optional.of(incomingRide));
        when(sharedRideGroupJpaRepository.findOpenGroups(any())).thenReturn(List.of(group));
        when(rideRequestJpaRepository.findById(anchorRequest.getId())).thenReturn(Optional.of(anchorRequest));
        when(rideStopJpaRepository.findByRideRequestIdOrderByRequestSequenceNoAsc(anchorRequest.getId())).thenReturn(stops(anchorRequest, 28.6140, 77.2100, 28.6210, 77.2230));
        when(rideStopJpaRepository.findByRideRequestIdOrderByRequestSequenceNoAsc(incomingRequest.getId())).thenReturn(stops(incomingRequest, 28.6139, 77.2090, 28.6200, 77.2200));
        when(poolRouteEvaluator.evaluate(any())).thenReturn(List.of(plan));
        when(rideJpaRepository.findByBookingRequestId(anchorRequest.getId())).thenReturn(Optional.of(anchorRide));
        when(sharedRideGroupJpaRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(rideJpaRepository.findByBookingRequestIdForUpdate(anchorRequest.getId())).thenReturn(Optional.of(anchorRide));
        when(rideRequestJpaRepository.findByIdForUpdate(anchorRequest.getId())).thenReturn(Optional.of(anchorRequest));
        when(rideRequestJpaRepository.getReferenceById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return id.equals(anchorRequest.getId()) ? anchorRequest : incomingRequest;
        });
        when(rideJpaRepository.getReferenceById(anchorRide.getId())).thenReturn(anchorRide);

        PoolMatchDecision decision = sharedRideMatchingService.matchOrCreateGroup(incomingRequest.getId());

        assertThat(decision.matched()).isTrue();
        assertThat(decision.sharedRideGroupId()).isEqualTo(group.getId());
        assertThat(incomingRide.getSharedRideGroup()).isEqualTo(group);
        assertThat(group.getGroupStatus()).isEqualTo(SharedRideGroupStatus.OPEN);
        assertThat(group.getOccupiedSeatCount()).isEqualTo((short) 2);
        verify(sharedRideRouteStopJpaRepository).saveAll(any());
        verify(sharedRideCandidateJpaRepository).save(any());
    }

    private RideRequestEntity rideRequest(String requestId, String riderId, double pickupLat, double pickupLon, double dropLat, double dropLon) {
        RideRequestEntity request = new RideRequestEntity();
        request.setId(UUID.fromString(requestId));
        request.setRequestedRideType(RequestedRideType.SHARED);
        request.setRequestStatus(RideRequestStatusEntityType.SEARCHING_DRIVER);
        request.setSeatCount((short) 1);
        request.setOrigin(point(pickupLon, pickupLat));
        request.setDestination(point(dropLon, dropLat));
        RiderProfileEntity rider = new RiderProfileEntity();
        rider.setId(UUID.fromString(riderId));
        request.setRiderProfile(rider);
        FareQuoteEntity quote = new FareQuoteEntity();
        quote.setId(UUID.randomUUID());
        quote.setTotalAmount(new BigDecimal("180.00"));
        quote.setPoolingDiscountAmount(new BigDecimal("25.00"));
        quote.setDiscountAmount(BigDecimal.ZERO);
        request.setFareQuote(quote);
        return request;
    }

    private List<RideStopEntity> stops(RideRequestEntity request, double pickupLat, double pickupLon, double dropLat, double dropLon) {
        RideStopEntity pickup = new RideStopEntity();
        pickup.setId(UUID.randomUUID());
        pickup.setRideRequest(request);
        pickup.setRiderProfile(request.getRiderProfile());
        pickup.setStopType(StopType.PICKUP);
        pickup.setPassengerCount((short) 1);
        pickup.setStopPoint(point(pickupLon, pickupLat));
        pickup.setAddressLine("Pickup");
        pickup.setRequestSequenceNo(1);

        RideStopEntity drop = new RideStopEntity();
        drop.setId(UUID.randomUUID());
        drop.setRideRequest(request);
        drop.setRiderProfile(request.getRiderProfile());
        drop.setStopType(StopType.DROPOFF);
        drop.setPassengerCount((short) 1);
        drop.setStopPoint(point(dropLon, dropLat));
        drop.setAddressLine("Drop");
        drop.setRequestSequenceNo(2);
        return List.of(pickup, drop);
    }

    private PoolRouteStop poolStop(RideRequestEntity request, StopType stopType, RouteStopRole role, double lat, double lon) {
        return new PoolRouteStop(
                request.getId(),
                request.getRiderProfile().getId(),
                stopType,
                role,
                lat,
                lon,
                stopType == StopType.PICKUP ? "Pickup" : "Drop",
                "Locality",
                (short) 1
        );
    }

    private org.locationtech.jts.geom.Point point(double longitude, double latitude) {
        return new GeometryFactory().createPoint(new Coordinate(longitude, latitude));
    }
}
