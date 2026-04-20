package com.ridingplatform.pricing.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "fare_quote", schema = "pricing")
public class FareQuoteEntity extends AbstractJpaEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_status", nullable = false, length = 32)
    private FareQuoteStatus pricingStatus;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "base_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "distance_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal distanceFare;

    @Column(name = "duration_fare", nullable = false, precision = 12, scale = 2)
    private BigDecimal durationFare;

    @Column(name = "surge_multiplier", nullable = false, precision = 8, scale = 4)
    private BigDecimal surgeMultiplier;

    @Column(name = "surge_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal surgeAmount;

    @Column(name = "booking_fee", nullable = false, precision = 12, scale = 2)
    private BigDecimal bookingFee;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "toll_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal tollAmount;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "pooling_discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal poolingDiscountAmount;

    @Column(name = "rounding_adjustment", nullable = false, precision = 12, scale = 2)
    private BigDecimal roundingAdjustment;

    @Column(name = "waiting_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal waitingCharge;

    @Column(name = "cancellation_charge", nullable = false, precision = 12, scale = 2)
    private BigDecimal cancellationCharge;

    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "pricing_strategy_code", length = 64)
    private String pricingStrategyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pricing_rule_set_id")
    private PricingRuleSetEntity pricingRuleSet;

    @Column(name = "city_code", length = 32)
    private String cityCode;

    @Column(name = "zone_code", length = 64)
    private String zoneCode;

    @Column(name = "pricing_version")
    private Integer pricingVersion;

    @Column(name = "quoted_distance_meters")
    private Integer quotedDistanceMeters;

    @Column(name = "quoted_duration_seconds")
    private Integer quotedDurationSeconds;

    @Column(name = "finalized_distance_meters")
    private Integer finalizedDistanceMeters;

    @Column(name = "finalized_duration_seconds")
    private Integer finalizedDurationSeconds;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;
}
