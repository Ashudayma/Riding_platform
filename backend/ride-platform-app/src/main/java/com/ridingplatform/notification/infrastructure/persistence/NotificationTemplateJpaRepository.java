package com.ridingplatform.notification.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationTemplateJpaRepository extends JpaRepository<NotificationTemplateEntity, UUID> {

    @Query("""
            select template
            from NotificationTemplateEntity template
            where template.eventCode = :eventCode
              and template.channel = :channel
              and template.locale = :locale
              and template.active = true
              and template.deletedAt is null
            """)
    Optional<NotificationTemplateEntity> findActiveTemplate(
            @Param("eventCode") String eventCode,
            @Param("channel") NotificationChannel channel,
            @Param("locale") String locale
    );
}
