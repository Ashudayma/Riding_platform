package com.ridingplatform.tracking.infrastructure.persistence;

import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "driver_location_history", schema = "tracking")
public class DriverLocationHistoryEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "driver_profile_id", nullable = false)
    private DriverProfileEntity driverProfile;

    @ManyToOne
    @JoinColumn(name = "ride_id")
    private RideEntity ride;

    @Column(nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "heading_degrees", precision = 6, scale = 2)
    private BigDecimal headingDegrees;

    @Column(name = "speed_kph", precision = 8, scale = 2)
    private BigDecimal speedKph;

    @Column(name = "accuracy_meters", precision = 8, scale = 2)
    private BigDecimal accuracyMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_provider", length = 32)
    private LocationProviderType locationProvider;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
