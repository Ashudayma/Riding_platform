package com.ridingplatform.driver.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverAvailabilityJpaRepository extends JpaRepository<DriverAvailabilityEntity, UUID> {

    Optional<DriverAvailabilityEntity> findByDriverProfileId(UUID driverProfileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select availability
            from DriverAvailabilityEntity availability
            join fetch availability.driverProfile driverProfile
            left join fetch driverProfile.currentVehicle vehicle
            where availability.driverProfile.id = :driverProfileId
              and availability.deletedAt is null
            """)
    Optional<DriverAvailabilityEntity> findByDriverProfileIdForUpdate(@Param("driverProfileId") UUID driverProfileId);

    @Query(value = """
            SELECT da.*
            FROM driver.driver_availability da
            JOIN driver.driver_profile dp ON dp.id = da.driver_profile_id
            JOIN driver.vehicle v ON v.id = dp.current_vehicle_id
            WHERE da.deleted_at IS NULL
              AND da.online_status = 'ONLINE'
              AND (:availabilityStatus IS NULL OR da.availability_status = CAST(:availabilityStatus AS varchar))
              AND (:vehicleType IS NULL OR v.vehicle_type = CAST(:vehicleType AS varchar))
              AND (:excludeActiveRide = FALSE OR da.current_ride_id IS NULL)
              AND (:minimumRating IS NULL OR dp.average_rating >= :minimumRating)
              AND (:excludeRiskBlocked = FALSE OR dp.driver_status <> 'BLOCKED')
              AND ST_DWithin(da.last_location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusMeters)
            ORDER BY ST_Distance(da.last_location, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography)
            LIMIT :limit
            """, nativeQuery = true)
    List<DriverAvailabilityEntity> searchNearbyPostgis(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit,
            @Param("availabilityStatus") String availabilityStatus,
            @Param("vehicleType") String vehicleType,
            @Param("excludeActiveRide") boolean excludeActiveRide,
            @Param("minimumRating") BigDecimal minimumRating,
            @Param("excludeRiskBlocked") boolean excludeRiskBlocked
    );
}
