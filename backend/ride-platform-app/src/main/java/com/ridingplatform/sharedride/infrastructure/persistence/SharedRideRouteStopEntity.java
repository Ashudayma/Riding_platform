package com.ridingplatform.sharedride.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.ride.infrastructure.persistence.StopType;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "shared_ride_route_stop", schema = "sharedride")
public class SharedRideRouteStopEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shared_ride_group_id", nullable = false)
    private SharedRideGroupEntity sharedRideGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_request_id", nullable = false)
    private RideRequestEntity rideRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_profile_id", nullable = false)
    private RiderProfileEntity riderProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_type", nullable = false, length = 32)
    private StopType stopType;

    @Column(name = "route_sequence_no", nullable = false)
    private int routeSequenceNo;

    @Column(name = "stop_point", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point stopPoint;

    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    @Column(length = 120)
    private String locality;

    @Column(name = "passenger_count", nullable = false)
    private short passengerCount;

    @Column(name = "planned_arrival_at")
    private Instant plannedArrivalAt;

    @Column(name = "detour_seconds")
    private Integer detourSeconds;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}
