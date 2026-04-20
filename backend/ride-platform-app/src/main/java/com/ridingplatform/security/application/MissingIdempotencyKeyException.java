package com.ridingplatform.security.application;

public class MissingIdempotencyKeyException extends RuntimeException {

    public MissingIdempotencyKeyException(String message) {
        super(message);
    }
}
