package com.ridingplatform.notification.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryAttemptJpaRepository extends JpaRepository<NotificationDeliveryAttemptEntity, UUID> {

    List<NotificationDeliveryAttemptEntity> findByNotificationIdAndDeletedAtIsNullOrderByAttemptNoDesc(UUID notificationId);
}
