package com.ridingplatform.driver.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
@Table(name = "driver_availability", schema = "driver")
public class DriverAvailabilityEntity extends AbstractJpaEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_profile_id", nullable = false)
    private DriverProfileEntity driverProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 32)
    private AvailabilityStatus availabilityStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "online_status", nullable = false, length = 32)
    private OnlineStatus onlineStatus;

    @Column(name = "current_session_id", length = 128)
    private String currentSessionId;

    @Column(name = "available_seat_count", nullable = false)
    private short availableSeatCount;

    @Column(name = "current_ride_id")
    private UUID currentRideId;

    @Column(name = "last_location", columnDefinition = "geography(Point,4326)")
    private Point lastLocation;

    @Column(name = "last_location_accuracy_meters", precision = 8, scale = 2)
    private BigDecimal lastLocationAccuracyMeters;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "available_since")
    private Instant availableSince;

    @Column(name = "app_version", length = 32)
    private String appVersion;

    @Column(name = "device_platform", length = 32)
    private String devicePlatform;
}
