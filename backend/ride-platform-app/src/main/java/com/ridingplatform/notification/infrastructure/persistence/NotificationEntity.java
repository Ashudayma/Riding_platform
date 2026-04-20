package com.ridingplatform.notification.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification", schema = "notification")
public class NotificationEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfileEntity userProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private RideEntity ride;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 32)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_code", length = 64)
    private NotificationEventCode eventCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 32)
    private NotificationDeliveryStatus deliveryStatus;

    @Column(name = "template_key", length = 100)
    private String templateKey;

    @Column(length = 200)
    private String title;

    @Column(length = 2000)
    private String body;

    @Column(name = "payload_json", columnDefinition = "jsonb")
    private String payloadJson;

    @Column(name = "provider_message_ref", length = 128)
    private String providerMessageRef;

    @Column(name = "provider_key", length = 64)
    private String providerKey;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;
}
