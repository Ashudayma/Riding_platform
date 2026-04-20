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
@Table(name = "shared_ride_group", schema = "sharedride")
public class SharedRideGroupEntity extends AbstractJpaEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "group_status", nullable = false, length = 32)
    private SharedRideGroupStatus groupStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anchor_ride_request_id")
    private RideRequestEntity anchorRideRequest;

    @Column(name = "max_seat_capacity", nullable = false)
    private short maxSeatCapacity;

    @Column(name = "occupied_seat_count", nullable = false)
    private short occupiedSeatCount;

    @Column(name = "route_distance_meters")
    private Integer routeDistanceMeters;

    @Column(name = "route_duration_seconds")
    private Integer routeDurationSeconds;

    @Column(name = "detour_seconds")
    private Integer detourSeconds;

    @Column(name = "pooling_savings_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal poolingSavingsAmount;

    @Column(name = "formed_at")
    private Instant formedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "closed_at")
    private Instant closedAt;
}
