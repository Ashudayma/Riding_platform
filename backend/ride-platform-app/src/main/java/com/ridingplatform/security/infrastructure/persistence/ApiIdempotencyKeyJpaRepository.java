package com.ridingplatform.security.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiIdempotencyKeyJpaRepository extends JpaRepository<ApiIdempotencyKeyEntity, UUID> {

    Optional<ApiIdempotencyKeyEntity> findByActorSubjectAndIdempotencyKey(String actorSubject, String idempotencyKey);

    void deleteByExpiresAtBefore(Instant cutoff);
}
