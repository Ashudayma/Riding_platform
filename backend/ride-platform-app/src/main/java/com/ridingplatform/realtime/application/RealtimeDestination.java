package com.ridingplatform.realtime.application;

public final class RealtimeDestination {

    public static final String RIDER_RIDE_QUEUE = "/queue/rides";
    public static final String DRIVER_ASSIGNMENT_QUEUE = "/queue/assignments";
    public static final String DRIVER_RIDE_QUEUE = "/queue/driver-rides";
    public static final String RIDE_LOCATION_QUEUE = "/queue/ride-location";
    public static final String SESSION_STATE_QUEUE = "/queue/session-state";

    private RealtimeDestination() {
    }
}
