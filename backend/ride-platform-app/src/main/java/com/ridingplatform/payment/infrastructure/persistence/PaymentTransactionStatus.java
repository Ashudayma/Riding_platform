package com.ridingplatform.payment.infrastructure.persistence;

public enum PaymentTransactionStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    VOIDED
}
