package com.ridingplatform.ride.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.VehicleEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupEntity;
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
@Table(name = "ride", schema = "ride")
public class RideEntity extends AbstractJpaEntity {

    @Column(name = "public_ride_code", nullable = false, length = 32)
    private String publicRideCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_request_id")
    private RideRequestEntity bookingRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_rider_profile_id")
    private RiderProfileEntity bookingRiderProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_profile_id")
    private DriverProfileEntity driverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private VehicleEntity vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_ride_group_id")
    private SharedRideGroupEntity sharedRideGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type", nullable = false, length = 32)
    private RequestedRideType rideType;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 32)
    private RideLifecycleStatus lifecycleStatus;

    @Column(name = "current_stop_sequence")
    private Integer currentStopSequence;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "pickup_started_at")
    private Instant pickupStartedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_actor_type", length = 32)
    private CancellationActorType cancellationActorType;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_fare_quote_id")
    private FareQuoteEntity finalFareQuote;
}
