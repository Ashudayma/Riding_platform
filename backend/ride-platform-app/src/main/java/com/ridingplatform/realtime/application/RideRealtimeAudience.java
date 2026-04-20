package com.ridingplatform.realtime.application;

import java.util.UUID;

public record RideRealtimeAudience(
        UUID rideRequestId,
        UUID rideId,
        UUID riderSubject,
        UUID driverSubject
) {
}
