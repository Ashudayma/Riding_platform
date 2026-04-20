package com.ridingplatform.sharedride.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SharedRideGroupJpaRepository extends JpaRepository<SharedRideGroupEntity, UUID> {

    @Query("""
            select group
            from SharedRideGroupEntity group
            where group.deletedAt is null
              and (:status is null or group.groupStatus = :status)
            """)
    Page<SharedRideGroupEntity> searchAdmin(@Param("status") SharedRideGroupStatus status, Pageable pageable);

    @Query("""
            select group
            from SharedRideGroupEntity group
            join fetch group.anchorRideRequest anchor
            where group.groupStatus in :statuses
              and group.deletedAt is null
              and group.occupiedSeatCount < group.maxSeatCapacity
            order by group.createdAt asc
            """)
    List<SharedRideGroupEntity> findOpenGroups(@Param("statuses") Collection<SharedRideGroupStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select group
            from SharedRideGroupEntity group
            left join fetch group.anchorRideRequest
            where group.id = :id
              and group.deletedAt is null
            """)
    java.util.Optional<SharedRideGroupEntity> findByIdForUpdate(@Param("id") UUID id);
}
