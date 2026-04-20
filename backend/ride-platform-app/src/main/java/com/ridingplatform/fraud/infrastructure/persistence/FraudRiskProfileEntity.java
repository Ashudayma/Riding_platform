package com.ridingplatform.fraud.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.fraud.domain.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "risk_profile", schema = "fraud")
public class FraudRiskProfileEntity extends AbstractJpaEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 32)
    private FraudSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "aggregate_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal aggregateScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(name = "active_flag_count", nullable = false)
    private int activeFlagCount;

    @Column(name = "derived_blocked", nullable = false)
    private boolean derivedBlocked;

    @Column(name = "manual_blocked", nullable = false)
    private boolean manualBlocked;

    @Column(name = "blocked", nullable = false)
    private boolean blocked;

    @Column(name = "blocked_reason", length = 255)
    private String blockedReason;

    @Column(name = "last_signal_at")
    private Instant lastSignalAt;

    @Column(name = "last_assessed_at")
    private Instant lastAssessedAt;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}
