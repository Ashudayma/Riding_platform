package com.ridingplatform.fraud.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraudFlagJpaRepository extends JpaRepository<FraudFlagEntity, UUID> {

    List<FraudFlagEntity> findBySubjectTypeAndSubjectIdOrderByCreatedAtDesc(FraudSubjectType subjectType, UUID subjectId);

    List<FraudFlagEntity> findByFlagStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(List<FraudFlagStatus> statuses);

    List<FraudFlagEntity> findBySubjectTypeAndSubjectIdAndFlagStatusInAndDeletedAtIsNullOrderByCreatedAtDesc(
            FraudSubjectType subjectType,
            UUID subjectId,
            List<FraudFlagStatus> statuses
    );

    boolean existsBySubjectTypeAndSubjectIdAndRuleCodeAndFlagStatusInAndDeletedAtIsNull(
            FraudSubjectType subjectType,
            UUID subjectId,
            String ruleCode,
            List<FraudFlagStatus> statuses
    );

    @Query("""
            select flag
            from FraudFlagEntity flag
            where flag.deletedAt is null
              and (:subjectType is null or flag.subjectType = :subjectType)
              and (:flagStatus is null or flag.flagStatus = :flagStatus)
            """)
    Page<FraudFlagEntity> searchAdmin(
            @Param("subjectType") FraudSubjectType subjectType,
            @Param("flagStatus") FraudFlagStatus flagStatus,
            Pageable pageable
    );
}
