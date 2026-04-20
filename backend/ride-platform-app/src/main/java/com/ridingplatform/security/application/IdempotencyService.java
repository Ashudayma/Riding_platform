package com.ridingplatform.security.application;

import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.config.SecurityProperties;
import com.ridingplatform.security.infrastructure.persistence.ApiIdempotencyKeyEntity;
import com.ridingplatform.security.infrastructure.persistence.ApiIdempotencyKeyJpaRepository;
import com.ridingplatform.security.infrastructure.persistence.IdempotencyProcessingStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final ApiIdempotencyKeyJpaRepository repository;
    private final IdGenerator idGenerator;
    private final SecurityProperties securityProperties;
    private final Clock clock;

    public IdempotencyService(
            ApiIdempotencyKeyJpaRepository repository,
            IdGenerator idGenerator,
            SecurityProperties securityProperties,
            Clock clock
    ) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.securityProperties = securityProperties;
        this.clock = clock;
    }

    @Transactional
    public Optional<StoredIdempotentResponse> begin(String actorSubject, String idempotencyKey, String method, String path, String requestHash) {
        Instant now = Instant.now(clock);
        repository.deleteByExpiresAtBefore(now.minus(1, ChronoUnit.HOURS));
        Optional<ApiIdempotencyKeyEntity> existing = repository.findByActorSubjectAndIdempotencyKey(actorSubject, idempotencyKey);
        if (existing.isPresent()) {
            ApiIdempotencyKeyEntity entity = existing.get();
            if (!entity.getRequestHash().equals(requestHash) || !entity.getRequestPath().equals(path) || !entity.getRequestMethod().equals(method)) {
                throw new IdempotencyConflictException("Idempotency key already used with a different request payload");
            }
            if (entity.getProcessingStatus() == IdempotencyProcessingStatus.IN_PROGRESS) {
                throw new IdempotencyConflictException("A request with this idempotency key is already being processed");
            }
            if (entity.getProcessingStatus() == IdempotencyProcessingStatus.COMPLETED && entity.getResponseStatus() != null) {
                return Optional.of(new StoredIdempotentResponse(entity.getResponseStatus(), entity.getResponseBody()));
            }
            entity.setProcessingStatus(IdempotencyProcessingStatus.IN_PROGRESS);
            entity.setUpdatedAt(now);
            repository.save(entity);
            return Optional.empty();
        }

        ApiIdempotencyKeyEntity entity = new ApiIdempotencyKeyEntity();
        entity.setId(idGenerator.nextId());
        entity.setIdempotencyKey(idempotencyKey);
        entity.setActorSubject(actorSubject);
        entity.setRequestMethod(method);
        entity.setRequestPath(path);
        entity.setRequestHash(requestHash);
        entity.setProcessingStatus(IdempotencyProcessingStatus.IN_PROGRESS);
        entity.setExpiresAt(now.plus(securityProperties.idempotency().ttlHours(), ChronoUnit.HOURS));
        repository.save(entity);
        return Optional.empty();
    }

    @Transactional
    public void complete(String actorSubject, String idempotencyKey, int responseStatus, String responseBody) {
        repository.findByActorSubjectAndIdempotencyKey(actorSubject, idempotencyKey)
                .ifPresent(entity -> {
                    entity.setResponseStatus(responseStatus);
                    entity.setResponseBody(responseBody);
                    entity.setProcessingStatus(responseStatus >= 200 && responseStatus < 300
                            ? IdempotencyProcessingStatus.COMPLETED
                            : IdempotencyProcessingStatus.FAILED);
                    repository.save(entity);
                });
    }
}
