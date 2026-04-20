package com.ridingplatform.fraud.domain;

public enum FraudSignalType {
    RIDE_CANCELLED,
    PAYMENT_FAILED,
    DRIVER_LOCATION_UPDATE,
    RIDE_COMPLETED,
    PROMO_REDEEMED,
    ACCOUNT_DEVICE_ANOMALY,
    ROUTE_DEVIATION,
    MANUAL_REVIEW
}
