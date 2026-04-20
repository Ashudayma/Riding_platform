package com.ridingplatform.notification.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByUserProfileIdOrderByCreatedAtDesc(UUID userProfileId);

    @Query("""
            select notification
            from NotificationEntity notification
            where notification.userProfile.id = :userProfileId
              and notification.deletedAt is null
            order by notification.createdAt desc
            """)
    List<NotificationEntity> findRecentForUser(@Param("userProfileId") UUID userProfileId);

    @Query("""
            select notification
            from NotificationEntity notification
            where notification.deliveryStatus = com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryStatus.FAILED
              and notification.retryCount < :maxRetryAttempts
              and notification.nextRetryAt <= CURRENT_TIMESTAMP
              and notification.deletedAt is null
            order by notification.nextRetryAt asc
            """)
    List<NotificationEntity> findRetryable(@Param("maxRetryAttempts") int maxRetryAttempts);
}
