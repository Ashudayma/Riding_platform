package com.ridingplatform.notification.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.notification.domain.NotificationProviderStatus;
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
@Table(name = "notification_delivery_attempt", schema = "notification")
public class NotificationDeliveryAttemptEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private NotificationEntity notification;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "provider_key", length = 64)
    private String providerKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_status", nullable = false, length = 32)
    private NotificationProviderStatus providerStatus;

    @Column(name = "response_payload_json", columnDefinition = "jsonb")
    private String responsePayloadJson;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
