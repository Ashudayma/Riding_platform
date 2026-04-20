package com.ridingplatform.sharedride.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shared_ride_candidate", schema = "sharedride")
public class SharedRideCandidateEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_ride_request_id", nullable = false)
    private RideRequestEntity baseRideRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_ride_request_id", nullable = false)
    private RideRequestEntity candidateRideRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_group_id")
    private SharedRideGroupEntity proposedGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_status", nullable = false, length = 32)
    private PoolingEvaluationStatus evaluationStatus;

    @Column(name = "compatibility_score", nullable = false, precision = 8, scale = 4)
    private BigDecimal compatibilityScore;

    @Column(name = "overlap_distance_meters")
    private Integer overlapDistanceMeters;

    @Column(name = "detour_delta_seconds")
    private Integer detourDeltaSeconds;

    @Column(name = "estimated_savings_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedSavingsAmount;

    @Column(name = "seat_fit", nullable = false)
    private boolean seatFit;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "evaluation_metadata_json", columnDefinition = "jsonb")
    private String evaluationMetadataJson;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
