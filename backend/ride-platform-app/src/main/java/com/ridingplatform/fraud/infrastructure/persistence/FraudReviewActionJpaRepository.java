package com.ridingplatform.fraud.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudReviewActionJpaRepository extends JpaRepository<FraudReviewActionEntity, UUID> {

    List<FraudReviewActionEntity> findByFraudFlagIdAndDeletedAtIsNullOrderByOccurredAtDesc(UUID fraudFlagId);
}
