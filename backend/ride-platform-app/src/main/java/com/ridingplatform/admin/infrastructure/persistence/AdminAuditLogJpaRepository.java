package com.ridingplatform.admin.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogJpaRepository extends JpaRepository<AdminAuditLogEntity, UUID> {

    List<AdminAuditLogEntity> findTop100ByTargetTypeAndTargetIdOrderByOccurredAtDesc(String targetType, UUID targetId);

    Page<AdminAuditLogEntity> findByTargetTypeOrderByOccurredAtDesc(String targetType, Pageable pageable);
}
