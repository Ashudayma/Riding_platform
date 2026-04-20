package com.ridingplatform.fraud.application;

public class FraudNotFoundException extends RuntimeException {

    public FraudNotFoundException(String message) {
        super(message);
    }
}
