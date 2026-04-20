package com.ridingplatform.payment.infrastructure.persistence;

public enum PaymentTransactionType {
    AUTHORIZATION,
    CAPTURE,
    SALE,
    REFUND,
    VOID
}
