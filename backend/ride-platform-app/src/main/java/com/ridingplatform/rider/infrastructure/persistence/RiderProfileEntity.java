package com.ridingplatform.rider.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rider_profile", schema = "rider")
public class RiderProfileEntity extends AbstractJpaEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfileEntity userProfile;

    @Column(name = "rider_code", nullable = false, length = 32)
    private String riderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "rider_status", nullable = false, length = 32)
    private RiderStatus riderStatus;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private java.math.BigDecimal averageRating;

    @Column(name = "lifetime_ride_count", nullable = false)
    private int lifetimeRideCount;

    @Column(name = "cancellation_count", nullable = false)
    private int cancellationCount;

    @Column(name = "no_show_count", nullable = false)
    private int noShowCount;

    @Column(name = "fraud_hold", nullable = false)
    private boolean fraudHold;

    @Column(name = "preferred_language", length = 16)
    private String preferredLanguage;

    @Column(name = "accessibility_requirements", length = 255)
    private String accessibilityRequirements;

    @Column(name = "emergency_contact_name", length = 120)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 32)
    private String emergencyContactPhone;

    @Column(length = 500)
    private String notes;
}
