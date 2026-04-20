package com.ridingplatform.sharedride.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedRideCandidateJpaRepository extends JpaRepository<SharedRideCandidateEntity, UUID> {

    List<SharedRideCandidateEntity> findByBaseRideRequestIdOrderByCompatibilityScoreDesc(UUID baseRideRequestId);
}
