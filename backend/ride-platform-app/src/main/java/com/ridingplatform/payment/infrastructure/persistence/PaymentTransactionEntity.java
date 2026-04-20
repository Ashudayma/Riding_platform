package com.ridingplatform.payment.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
@Table(name = "payment_transaction", schema = "payment")
public class PaymentTransactionEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_request_id")
    private RideRequestEntity rideRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private RideEntity ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethodEntity paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_provider", nullable = false, length = 32)
    private PaymentProvider paymentProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 32)
    private PaymentTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 32)
    private PaymentTransactionStatus transactionStatus;

    @Column(name = "provider_transaction_ref", length = 128)
    private String providerTransactionRef;

    @Column(name = "provider_idempotency_key", length = 128)
    private String providerIdempotencyKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "authorized_at")
    private Instant authorizedAt;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;
}
