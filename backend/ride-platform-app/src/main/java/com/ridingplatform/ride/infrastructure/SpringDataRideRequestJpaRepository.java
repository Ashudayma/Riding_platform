package com.ridingplatform.ride.infrastructure;

import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestStatusEntityType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface SpringDataRideRequestJpaRepository extends JpaRepository<RideRequestEntity, UUID> {

    List<RideRequestEntity> findTop50ByRiderProfileIdAndDeletedAtIsNullOrderByRequestedAtDesc(UUID riderProfileId);

    List<RideRequestEntity> findByRiderProfileIdAndRequestStatusAndDeletedAtIsNull(UUID riderProfileId, RideRequestStatusEntityType requestStatus);

    @Query("""
            select rideRequest
            from RideRequestEntity rideRequest
            where rideRequest.deletedAt is null
              and (:riderProfileId is null or rideRequest.riderProfile.id = :riderProfileId)
              and (:requestStatus is null or rideRequest.requestStatus = :requestStatus)
              and (:rideType is null or rideRequest.requestedRideType = :rideType)
              and (:fromInstant is null or rideRequest.requestedAt >= :fromInstant)
              and (:toInstant is null or rideRequest.requestedAt <= :toInstant)
            """)
    Page<RideRequestEntity> searchAdmin(
            @Param("riderProfileId") UUID riderProfileId,
            @Param("requestStatus") RideRequestStatusEntityType requestStatus,
            @Param("rideType") com.ridingplatform.ride.infrastructure.persistence.RequestedRideType rideType,
            @Param("fromInstant") java.time.Instant fromInstant,
            @Param("toInstant") java.time.Instant toInstant,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select rideRequest
            from RideRequestEntity rideRequest
            where rideRequest.id = :id
              and rideRequest.deletedAt is null
            """)
    java.util.Optional<RideRequestEntity> findByIdForUpdate(@Param("id") UUID id);
}
