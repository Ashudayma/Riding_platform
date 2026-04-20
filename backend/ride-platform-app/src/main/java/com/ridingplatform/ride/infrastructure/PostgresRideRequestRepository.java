package com.ridingplatform.ride.infrastructure;

import com.ridingplatform.common.persistence.GeoFactory;
import com.ridingplatform.ride.domain.RideRequest;
import com.ridingplatform.ride.domain.RideRequestRepository;
import com.ridingplatform.ride.infrastructure.persistence.RequestedRideType;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresRideRequestRepository implements RideRequestRepository {

    private final SpringDataRideRequestJpaRepository repository;
    private final EntityManager entityManager;

    public PostgresRideRequestRepository(
            SpringDataRideRequestJpaRepository repository,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public RideRequest save(RideRequest rideRequest) {
        RideRequestEntity entity = new RideRequestEntity();
        entity.setId(rideRequest.getId());
        entity.setRiderProfile(entityManager.getReference(RiderProfileEntity.class, rideRequest.riderId()));
        entity.setRequestedRideType(rideRequest.rideType() == com.ridingplatform.ride.domain.RideType.SHARED
                ? RequestedRideType.SHARED
                : RequestedRideType.STANDARD);
        entity.setRequestStatus(RideRequestStatusEntityType.valueOf(rideRequest.status().name()));
        entity.setSeatCount((short) 1);
        entity.setOrigin(GeoFactory.point(rideRequest.origin().latitude(), rideRequest.origin().longitude()));
        entity.setDestination(GeoFactory.point(rideRequest.destination().latitude(), rideRequest.destination().longitude()));
        entity.setOriginAddress(rideRequest.origin().address());
        entity.setDestinationAddress(rideRequest.destination().address());
        entity.setRequestedAt(rideRequest.getCreatedAt());
        entity.setCreatedAt(rideRequest.getCreatedAt());
        entity.setUpdatedAt(rideRequest.getUpdatedAt());
        repository.save(entity);
        return rideRequest;
    }
}
