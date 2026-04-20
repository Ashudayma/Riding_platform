package com.ridingplatform.fraud.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraudRiskProfileJpaRepository extends JpaRepository<FraudRiskProfileEntity, UUID> {

    Optional<FraudRiskProfileEntity> findBySubjectTypeAndSubjectId(FraudSubjectType subjectType, UUID subjectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select profile
            from FraudRiskProfileEntity profile
            where profile.subjectType = :subjectType
              and profile.subjectId = :subjectId
              and profile.deletedAt is null
            """)
    Optional<FraudRiskProfileEntity> findBySubjectTypeAndSubjectIdForUpdate(
            @Param("subjectType") FraudSubjectType subjectType,
            @Param("subjectId") UUID subjectId
    );

    List<FraudRiskProfileEntity> findTop100ByBlockedTrueAndDeletedAtIsNullOrderByUpdatedAtDesc();

    Page<FraudRiskProfileEntity> findByBlockedAndDeletedAtIsNull(boolean blocked, Pageable pageable);
}
