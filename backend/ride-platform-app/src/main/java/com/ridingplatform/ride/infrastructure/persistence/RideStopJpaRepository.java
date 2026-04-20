package com.ridingplatform.ride.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideStopJpaRepository extends JpaRepository<RideStopEntity, UUID> {

    List<RideStopEntity> findByRideRequestIdOrderByRequestSequenceNoAsc(UUID rideRequestId);

    List<RideStopEntity> findByRideIdOrderByRideSequenceNoAsc(UUID rideId);
}
