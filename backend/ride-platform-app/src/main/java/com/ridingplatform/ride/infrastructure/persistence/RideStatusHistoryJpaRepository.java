package com.ridingplatform.ride.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideStatusHistoryJpaRepository extends JpaRepository<RideStatusHistoryEntity, UUID> {

    List<RideStatusHistoryEntity> findByRideIdOrderByChangedAtDesc(UUID rideId);

    List<RideStatusHistoryEntity> findByRideRequestIdOrderByChangedAtDesc(UUID rideRequestId);
}
