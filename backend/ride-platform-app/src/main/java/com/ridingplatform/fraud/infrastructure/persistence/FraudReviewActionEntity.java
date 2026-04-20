package com.ridingplatform.fraud.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.fraud.domain.FraudReviewActionType;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
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
@Table(name = "fraud_review_action", schema = "fraud")
public class FraudReviewActionEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fraud_flag_id", nullable = false)
    private FraudFlagEntity fraudFlag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_profile_id")
    private UserProfileEntity actorUserProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 32)
    private FraudReviewActionType actionType;

    @Column(length = 1000)
    private String note;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
