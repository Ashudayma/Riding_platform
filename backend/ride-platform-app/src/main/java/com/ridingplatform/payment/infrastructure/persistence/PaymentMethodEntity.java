package com.ridingplatform.payment.infrastructure.persistence;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payment_method", schema = "payment")
public class PaymentMethodEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_profile_id", nullable = false)
    private RiderProfileEntity riderProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false, length = 32)
    private PaymentProvider paymentProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_type", nullable = false, length = 32)
    private PaymentMethodType paymentMethodType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method_status", nullable = false, length = 32)
    private PaymentMethodStatus paymentMethodStatus;

    @Column(name = "provider_customer_ref", length = 128)
    private String providerCustomerRef;

    @Column(name = "provider_payment_method_ref", length = 128)
    private String providerPaymentMethodRef;

    @Column(name = "card_brand", length = 32)
    private String cardBrand;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "expiry_month")
    private Short expiryMonth;

    @Column(name = "expiry_year")
    private Short expiryYear;

    @Column(name = "billing_country_code", length = 3)
    private String billingCountryCode;

    @Column(name = "is_default", nullable = false)
    private boolean defaultMethod;
}
