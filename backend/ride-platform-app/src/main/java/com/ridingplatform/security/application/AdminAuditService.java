package com.ridingplatform.security.application;

import com.ridingplatform.admin.infrastructure.persistence.AdminAuditLogEntity;
import com.ridingplatform.admin.infrastructure.persistence.AdminAuditLogJpaRepository;
import com.ridingplatform.admin.infrastructure.persistence.AuditResultStatus;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final AdminAuditLogJpaRepository repository;
    private final IdGenerator idGenerator;
    private final EntityManager entityManager;
    private final Clock clock;

    public AdminAuditService(
            AdminAuditLogJpaRepository repository,
            IdGenerator idGenerator,
            EntityManager entityManager,
            Clock clock
    ) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.entityManager = entityManager;
        this.clock = clock;
    }

    @Transactional
    public void log(
            Optional<CurrentActor> actor,
            String actionCode,
            String targetType,
            UUID targetId,
            AuditResultStatus resultStatus,
            String requestId,
            String traceId,
            String sourceIp,
            String userAgent,
            String metadataJson
    ) {
        AdminAuditLogEntity entity = new AdminAuditLogEntity();
        entity.setId(idGenerator.nextId());
        actor.flatMap(currentActor -> Optional.ofNullable(currentActor.userProfileId()))
                .ifPresent(userProfileId -> entity.setActorUserProfile(entityManager.getReference(UserProfileEntity.class, userProfileId)));
        entity.setActionCode(actionCode);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setResultStatus(resultStatus);
        entity.setRequestId(requestId);
        entity.setTraceId(traceId);
        entity.setSourceIp(sourceIp);
        entity.setUserAgent(userAgent);
        entity.setMetadataJson(metadataJson);
        Instant now = Instant.now(clock);
        entity.setOccurredAt(now);
        entity.setCreatedAt(now);
        repository.save(entity);
    }
}
