package com.ridingplatform.notification.application;

import java.util.UUID;

public record RideNotificationAudience(
        UUID rideRequestId,
        UUID rideId,
        UUID riderUserProfileId,
        UUID driverUserProfileId,
        String driverDisplayName,
        String rideCode
) {
}
