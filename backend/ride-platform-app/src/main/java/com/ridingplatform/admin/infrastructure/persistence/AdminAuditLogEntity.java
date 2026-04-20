package com.ridingplatform.admin.infrastructure.persistence;

import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "admin_audit_log", schema = "admin")
public class AdminAuditLogEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "actor_user_profile_id")
    private UserProfileEntity actorUserProfile;

    @Column(name = "action_code", nullable = false, length = 100)
    private String actionCode;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 32)
    private AuditResultStatus resultStatus;

    @Column(name = "request_id", length = 128)
    private String requestId;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "source_ip", length = 64)
    private String sourceIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
