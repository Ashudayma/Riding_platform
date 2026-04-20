package com.ridingplatform.driver.application;

public record DriverAvailabilityCommand(
        boolean online,
        short availableSeatCount,
        String sessionId,
        String appVersion,
        String devicePlatform
) {
}
