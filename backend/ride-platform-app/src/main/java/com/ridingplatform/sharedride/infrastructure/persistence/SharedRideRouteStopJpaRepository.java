package com.ridingplatform.sharedride.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedRideRouteStopJpaRepository extends JpaRepository<SharedRideRouteStopEntity, UUID> {

    List<SharedRideRouteStopEntity> findBySharedRideGroupIdOrderByRouteSequenceNoAsc(UUID sharedRideGroupId);

    void deleteBySharedRideGroupId(UUID sharedRideGroupId);
}
