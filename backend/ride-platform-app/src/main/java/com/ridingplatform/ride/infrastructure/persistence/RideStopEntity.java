package com.ridingplatform.ride.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
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
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ride_stop", schema = "ride")
public class RideStopEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_request_id", nullable = false)
    private RideRequestEntity rideRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private RideEntity ride;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_profile_id", nullable = false)
    private RiderProfileEntity riderProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false, length = 32)
    private StopType stopType;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_status", nullable = false, length = 32)
    private StopStatus stopStatus;

    @Column(name = "request_sequence_no", nullable = false)
    private int requestSequenceNo;

    @Column(name = "ride_sequence_no")
    private Integer rideSequenceNo;

    @Column(name = "stop_point", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point stopPoint;

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(length = 120)
    private String locality;

    @Column(length = 16)
    private String geohash;

    @Column(name = "passenger_count", nullable = false)
    private short passengerCount;

    @Column(name = "planned_arrival_at")
    private Instant plannedArrivalAt;

    @Column(name = "actual_arrival_at")
    private Instant actualArrivalAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "detour_seconds")
    private Integer detourSeconds;

    @Column(length = 500)
    private String notes;
}
