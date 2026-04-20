package com.ridingplatform.notification.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_template", schema = "notification")
public class NotificationTemplateEntity extends AbstractJpaEntity {

    @Column(name = "event_code", nullable = false, length = 64)
    private String eventCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "title_template", length = 255)
    private String titleTemplate;

    @Column(name = "body_template", nullable = false, length = 4000)
    private String bodyTemplate;

    @Column(nullable = false)
    private boolean active;
}
