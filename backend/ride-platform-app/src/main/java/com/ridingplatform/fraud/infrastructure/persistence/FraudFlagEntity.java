package com.ridingplatform.fraud.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.fraud.domain.RiskLevel;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "fraud_flag", schema = "fraud")
public class FraudFlagEntity extends AbstractJpaEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 32)
    private FraudSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskLevel severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_status", nullable = false, length = 32)
    private FraudFlagStatus flagStatus;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "risk_score", precision = 8, scale = 4)
    private BigDecimal riskScore;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "evidence_json", columnDefinition = "jsonb")
    private String evidenceJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_user_profile_id")
    private UserProfileEntity resolvedByUserProfile;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
