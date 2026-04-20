package com.ridingplatform.identity.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileJpaRepository extends JpaRepository<UserProfileEntity, UUID> {

    Optional<UserProfileEntity> findByKeycloakUserId(UUID keycloakUserId);

    Optional<UserProfileEntity> findByEmailIgnoreCase(String email);
}
