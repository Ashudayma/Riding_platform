package com.ridingplatform.fraud.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FraudSignalEventJpaRepository extends JpaRepository<FraudSignalEventEntity, UUID> {

    List<FraudSignalEventEntity> findTop20BySubjectTypeAndSubjectIdAndDeletedAtIsNullOrderByOccurredAtDesc(FraudSubjectType subjectType, UUID subjectId);
}
