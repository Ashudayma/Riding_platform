package com.ridingplatform.ride.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import com.ridingplatform.notification.application.RideNotificationAudience;
import com.ridingplatform.realtime.application.RideRealtimeAudience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RideJpaRepository extends JpaRepository<RideEntity, UUID> {

    List<RideEntity> findByBookingRiderProfileIdOrderByCreatedAtDesc(UUID bookingRiderProfileId);

    @Query("""
            select ride
            from RideEntity ride
            where ride.deletedAt is null
              and (:driverProfileId is null or ride.driverProfile.id = :driverProfileId)
              and (:lifecycleStatus is null or ride.lifecycleStatus = :lifecycleStatus)
              and (:rideType is null or ride.rideType = :rideType)
              and (:fromInstant is null or ride.createdAt >= :fromInstant)
              and (:toInstant is null or ride.createdAt <= :toInstant)
            """)
    Page<RideEntity> searchAdmin(
            @Param("driverProfileId") UUID driverProfileId,
            @Param("lifecycleStatus") RideLifecycleStatus lifecycleStatus,
            @Param("rideType") RequestedRideType rideType,
            @Param("fromInstant") java.time.Instant fromInstant,
            @Param("toInstant") java.time.Instant toInstant,
            Pageable pageable
    );

    java.util.Optional<RideEntity> findByBookingRequestId(UUID bookingRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ride
            from RideEntity ride
            left join fetch ride.driverProfile
            left join fetch ride.vehicle
            where ride.bookingRequest.id = :bookingRequestId
              and ride.deletedAt is null
            """)
    java.util.Optional<RideEntity> findByBookingRequestIdForUpdate(@Param("bookingRequestId") UUID bookingRequestId);

    @Query("""
            select new com.ridingplatform.realtime.application.RideRealtimeAudience(
                request.id,
                ride.id,
                riderUser.keycloakUserId,
                driverUser.keycloakUserId
            )
            from RideEntity ride
            join ride.bookingRequest request
            join ride.bookingRiderProfile riderProfile
            join riderProfile.userProfile riderUser
            left join ride.driverProfile driverProfile
            left join driverProfile.userProfile driverUser
            where request.id = :rideRequestId
              and ride.deletedAt is null
            """)
    java.util.Optional<RideRealtimeAudience> findRealtimeAudienceByRideRequestId(@Param("rideRequestId") UUID rideRequestId);

    @Query("""
            select new com.ridingplatform.realtime.application.RideRealtimeAudience(
                request.id,
                ride.id,
                riderUser.keycloakUserId,
                driverUser.keycloakUserId
            )
            from RideEntity ride
            join ride.bookingRequest request
            join ride.bookingRiderProfile riderProfile
            join riderProfile.userProfile riderUser
            left join ride.driverProfile driverProfile
            left join driverProfile.userProfile driverUser
            where ride.id = :rideId
              and ride.deletedAt is null
            """)
    java.util.Optional<RideRealtimeAudience> findRealtimeAudienceByRideId(@Param("rideId") UUID rideId);

    @Query("""
            select new com.ridingplatform.notification.application.RideNotificationAudience(
                request.id,
                ride.id,
                riderUser.id,
                driverUser.id,
                coalesce(driverUser.displayName, driverUser.firstName, 'Driver'),
                ride.publicRideCode
            )
            from RideEntity ride
            join ride.bookingRequest request
            join ride.bookingRiderProfile riderProfile
            join riderProfile.userProfile riderUser
            left join ride.driverProfile driverProfile
            left join driverProfile.userProfile driverUser
            where request.id = :rideRequestId
              and ride.deletedAt is null
            """)
    java.util.Optional<RideNotificationAudience> findNotificationAudienceByRideRequestId(@Param("rideRequestId") UUID rideRequestId);

    @Query("""
            select new com.ridingplatform.notification.application.RideNotificationAudience(
                request.id,
                ride.id,
                riderUser.id,
                driverUser.id,
                coalesce(driverUser.displayName, driverUser.firstName, 'Driver'),
                ride.publicRideCode
            )
            from RideEntity ride
            join ride.bookingRequest request
            join ride.bookingRiderProfile riderProfile
            join riderProfile.userProfile riderUser
            left join ride.driverProfile driverProfile
            left join driverProfile.userProfile driverUser
            where ride.id = :rideId
              and ride.deletedAt is null
            """)
    java.util.Optional<RideNotificationAudience> findNotificationAudienceByRideId(@Param("rideId") UUID rideId);
}
