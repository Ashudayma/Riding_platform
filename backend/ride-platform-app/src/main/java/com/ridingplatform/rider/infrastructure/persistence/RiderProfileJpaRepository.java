package com.ridingplatform.rider.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiderProfileJpaRepository extends JpaRepository<RiderProfileEntity, UUID> {

    Optional<RiderProfileEntity> findByUserProfileId(UUID userProfileId);

    @Query("""
            select rider
            from RiderProfileEntity rider
            where rider.deletedAt is null
              and (:status is null or rider.riderStatus = :status)
              and (:fraudHold is null or rider.fraudHold = :fraudHold)
            """)
    Page<RiderProfileEntity> searchAdmin(
            @Param("status") RiderStatus status,
            @Param("fraudHold") Boolean fraudHold,
            Pageable pageable
    );

    @Query("""
            select rider.userProfile.id
            from RiderProfileEntity rider
            where rider.id = :riderProfileId
              and rider.deletedAt is null
            """)
    Optional<UUID> findUserProfileIdByRiderProfileId(@Param("riderProfileId") UUID riderProfileId);
}
