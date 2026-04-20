package com.ridingplatform.ride.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.VehicleEntity;
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
@Table(name = "driver_assignment_attempt", schema = "ride")
public class DriverAssignmentAttemptEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_request_id", nullable = false)
    private RideRequestEntity rideRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false)
    private RideEntity ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_profile_id")
    private DriverProfileEntity driverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicle;

    @Column(name = "dispatch_round", nullable = false)
    private int dispatchRound;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 32)
    private DriverAssignmentAttemptStatus assignmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason_code", length = 64)
    private DriverAssignmentFailureReason failureReasonCode;

    @Column(name = "weighted_score", precision = 10, scale = 6)
    private BigDecimal weightedScore;

    @Column(name = "distance_meters", precision = 10, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "eta_seconds")
    private Integer etaSeconds;

    @Column(name = "score_breakdown_json", columnDefinition = "jsonb")
    private String scoreBreakdownJson;

    @Column(name = "assignment_token")
    private UUID assignmentToken;

    @Column(name = "offered_at", nullable = false)
    private Instant offeredAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "response_note", length = 255)
    private String responseNote;
}
