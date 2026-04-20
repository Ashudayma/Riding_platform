package com.ridingplatform.ride.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.payment.infrastructure.persistence.PaymentMethodEntity;
import com.ridingplatform.pricing.infrastructure.persistence.FareQuoteEntity;
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
@Table(name = "ride_request", schema = "ride")
public class RideRequestEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_profile_id", nullable = false)
    private RiderProfileEntity riderProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_ride_type", nullable = false, length = 32)
    private RequestedRideType requestedRideType;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_status", nullable = false, length = 32)
    private RideRequestStatusEntityType requestStatus;

    @Column(name = "seat_count", nullable = false)
    private short seatCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_vehicle_type", length = 32)
    private VehicleType requestedVehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_quote_id")
    private FareQuoteEntity fareQuote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethodEntity paymentMethod;

    @Column(name = "origin", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point origin;

    @Column(name = "destination", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point destination;

    @Column(name = "origin_address", nullable = false, length = 500)
    private String originAddress;

    @Column(name = "destination_address", nullable = false, length = 500)
    private String destinationAddress;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "scheduled_for")
    private Instant scheduledFor;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "matching_batch_key", length = 64)
    private String matchingBatchKey;

    @Column(length = 500)
    private String notes;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;
}
