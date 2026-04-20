package com.ridingplatform.driver.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "driver_profile", schema = "driver")
public class DriverProfileEntity extends AbstractJpaEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfileEntity userProfile;

    @Column(name = "driver_code", nullable = false, length = 32)
    private String driverCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "driver_status", nullable = false, length = 32)
    private DriverStatus driverStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 32)
    private DriverOnboardingStatus onboardingStatus;

    @Column(name = "license_number", nullable = false, length = 64)
    private String licenseNumber;

    @Column(name = "license_country_code", nullable = false, length = 3)
    private String licenseCountryCode;

    @Column(name = "license_expires_at", nullable = false)
    private LocalDate licenseExpiresAt;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "lifetime_trip_count", nullable = false)
    private int lifetimeTripCount;

    @Column(name = "acceptance_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal acceptanceRate;

    @Column(name = "cancellation_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal cancellationRate;

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "fraud_blocked", nullable = false)
    private boolean fraudBlocked;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_vehicle_id")
    private VehicleEntity currentVehicle;

    @Column(name = "background_check_completed_at")
    private Instant backgroundCheckCompletedAt;

    @Column(name = "approved_at")
    private Instant approvedAt;
}
