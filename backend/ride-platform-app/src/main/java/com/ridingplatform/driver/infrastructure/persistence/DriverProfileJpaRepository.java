package com.ridingplatform.driver.infrastructure.persistence;

import com.ridingplatform.driver.application.DriverDispatchCandidate;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverProfileJpaRepository extends JpaRepository<DriverProfileEntity, UUID> {

    Optional<DriverProfileEntity> findByUserProfileId(UUID userProfileId);

    @Query("""
            select driver
            from DriverProfileEntity driver
            where driver.deletedAt is null
              and (:status is null or driver.driverStatus = :status)
              and (:fraudBlocked is null or driver.fraudBlocked = :fraudBlocked)
            """)
    Page<DriverProfileEntity> searchAdmin(
            @Param("status") DriverStatus status,
            @Param("fraudBlocked") Boolean fraudBlocked,
            Pageable pageable
    );

    @Query("""
            select driver.userProfile.id
            from DriverProfileEntity driver
            where driver.id = :driverProfileId
              and driver.deletedAt is null
            """)
    Optional<UUID> findUserProfileIdByDriverProfileId(@Param("driverProfileId") UUID driverProfileId);

    @Query("""
            select new com.ridingplatform.driver.application.DriverDispatchCandidate(
                driverProfile.id,
                vehicle.id,
                vehicle.vehicleType,
                coalesce(vehicle.seatCapacity, 0),
                availability.availabilityStatus,
                availability.onlineStatus,
                availability.currentRideId,
                driverProfile.averageRating,
                driverProfile.acceptanceRate,
                driverProfile.cancellationRate,
                driverProfile.riskScore,
                case when driverProfile.driverStatus = com.ridingplatform.driver.infrastructure.persistence.DriverStatus.BLOCKED
                          or driverProfile.fraudBlocked = true then true else false end,
                availability.availableSince,
                availability.lastHeartbeatAt
            )
            from DriverAvailabilityEntity availability
            join availability.driverProfile driverProfile
            left join driverProfile.currentVehicle vehicle
            where driverProfile.id in :driverProfileIds
              and availability.deletedAt is null
              and driverProfile.deletedAt is null
            """)
    java.util.List<DriverDispatchCandidate> findDispatchCandidates(@Param("driverProfileIds") Collection<UUID> driverProfileIds);
}
