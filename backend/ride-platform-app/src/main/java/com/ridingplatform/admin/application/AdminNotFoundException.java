package com.ridingplatform.admin.application;

public class AdminNotFoundException extends RuntimeException {

    public AdminNotFoundException(String message) {
        super(message);
    }
}
