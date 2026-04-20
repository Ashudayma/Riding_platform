package com.ridingplatform.ride.application;

public class RideNotFoundException extends RuntimeException {

    public RideNotFoundException(String message) {
        super(message);
    }
}
