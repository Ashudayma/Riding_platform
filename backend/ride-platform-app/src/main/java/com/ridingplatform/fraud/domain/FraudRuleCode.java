package com.ridingplatform.fraud.domain;

public final class FraudRuleCode {

    public static final String REPEATED_CANCELLATIONS = "REPEATED_CANCELLATIONS";
    public static final String FAILED_PAYMENT_ATTEMPTS = "FAILED_PAYMENT_ATTEMPTS";
    public static final String GPS_SPOOFING = "GPS_SPOOFING";
    public static final String FAKE_TRIP_COMPLETION = "FAKE_TRIP_COMPLETION";
    public static final String COLLUSION_PATTERN = "COLLUSION_PATTERN";
    public static final String SHORT_REPEATED_TRIPS = "SHORT_REPEATED_TRIPS";
    public static final String ACCOUNT_DEVICE_ANOMALY = "ACCOUNT_DEVICE_ANOMALY";
    public static final String PROMO_ABUSE = "PROMO_ABUSE";
    public static final String ROUTE_DEVIATION = "ROUTE_DEVIATION";

    private FraudRuleCode() {
    }
}
