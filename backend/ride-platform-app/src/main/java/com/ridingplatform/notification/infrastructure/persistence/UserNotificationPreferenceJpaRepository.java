package com.ridingplatform.notification.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNotificationPreferenceJpaRepository extends JpaRepository<UserNotificationPreferenceEntity, UUID> {

    List<UserNotificationPreferenceEntity> findByUserProfileIdAndDeletedAtIsNullOrderByEventCodeAsc(UUID userProfileId);

    Optional<UserNotificationPreferenceEntity> findByUserProfileIdAndEventCodeAndChannelAndDeletedAtIsNull(
            UUID userProfileId,
            String eventCode,
            NotificationChannel channel
    );
}
