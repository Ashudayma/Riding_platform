package com.ridingplatform.ride.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DriverAssignmentAttemptJpaRepository extends JpaRepository<DriverAssignmentAttemptEntity, UUID> {

    List<DriverAssignmentAttemptEntity> findByRideRequestIdOrderByAttemptNoAsc(UUID rideRequestId);

    Optional<DriverAssignmentAttemptEntity> findTopByRideRequestIdOrderByAttemptNoDesc(UUID rideRequestId);

    boolean existsByRideRequestIdAndDriverProfileIdAndAssignmentStatusIn(
            UUID rideRequestId,
            UUID driverProfileId,
            Collection<DriverAssignmentAttemptStatus> statuses
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select attempt
            from DriverAssignmentAttemptEntity attempt
            left join fetch attempt.driverProfile
            left join fetch attempt.vehicle
            where attempt.id = :id
              and attempt.deletedAt is null
            """)
    Optional<DriverAssignmentAttemptEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            select attempt
            from DriverAssignmentAttemptEntity attempt
            where attempt.assignmentStatus = :status
              and attempt.expiresAt <= :cutoff
              and attempt.deletedAt is null
            order by attempt.expiresAt asc
            """)
    List<DriverAssignmentAttemptEntity> findTopExpiredByStatus(
            @Param("status") DriverAssignmentAttemptStatus status,
            @Param("cutoff") Instant cutoff,
            org.springframework.data.domain.Pageable pageable
    );

    Page<DriverAssignmentAttemptEntity> findByDeletedAtIsNull(org.springframework.data.domain.Pageable pageable);
}
