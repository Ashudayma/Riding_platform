package com.ridingplatform.ride.infrastructure.persistence;

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
@Table(name = "ride_status_history", schema = "ride")
public class RideStatusHistoryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "ride_request_id")
    private RideRequestEntity rideRequest;

    @ManyToOne
    @JoinColumn(name = "ride_id")
    private RideEntity ride;

    @Column(name = "previous_status", length = 32)
    private String previousStatus;

    @Column(name = "current_status", nullable = false, length = 32)
    private String currentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private StatusSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", length = 32)
    private StatusActorType actorType;

    @ManyToOne
    @JoinColumn(name = "actor_user_profile_id")
    private UserProfileEntity actorUserProfile;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @Column(length = 500)
    private String note;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
