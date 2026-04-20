package com.ridingplatform.pricing.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.domain.RideType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "pricing_rule_set", schema = "pricing")
public class PricingRuleSetEntity extends AbstractJpaEntity {

    @Column(name = "city_code", nullable = false, length = 32)
    private String cityCode;

    @Column(name = "zone_code", length = 64)
    private String zoneCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type", nullable = false, length = 32)
    private RideType rideType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", length = 32)
    private VehicleType vehicleType;

    @Column(name = "pricing_version", nullable = false)
    private int pricingVersion;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "minimum_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal minimumFare;

    @Column(name = "booking_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookingFee;

    @Column(name = "per_km_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal perKmRate;

    @Column(name = "per_minute_rate", nullable = false, precision = 12, scale = 4)
    private BigDecimal perMinuteRate;

    @Column(name = "per_stop_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal perStopCharge;

    @Column(name = "waiting_charge_per_minute", nullable = false, precision = 12, scale = 4)
    private BigDecimal waitingChargePerMinute;

    @Column(name = "cancellation_base_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal cancellationBaseCharge;

    @Column(name = "cancellation_per_km_charge", nullable = false, precision = 12, scale = 4)
    private BigDecimal cancellationPerKmCharge;

    @Column(name = "shared_discount_factor", nullable = false, precision = 8, scale = 4)
    private BigDecimal sharedDiscountFactor;

    @Column(name = "tax_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal taxPercentage;

    @Column(name = "surge_cap_multiplier", nullable = false, precision = 8, scale = 4)
    private BigDecimal surgeCapMultiplier;

    @Column(name = "night_surcharge_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal nightSurchargePercentage;

    @Column(name = "airport_surcharge_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal airportSurchargeAmount;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to")
    private Instant effectiveTo;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}
