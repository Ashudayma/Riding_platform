package com.ridingplatform.ride.domain;

import com.ridingplatform.common.domain.BaseEntity;
import java.time.Instant;
import java.util.UUID;

public class RideRequest extends BaseEntity {

    private final UUID riderId;
    private final RideType rideType;
    private final LocationPoint origin;
    private final LocationPoint destination;
    private RideStatus status;

    public RideRequest(
            UUID id,
            UUID riderId,
            RideType rideType,
            LocationPoint origin,
            LocationPoint destination,
            RideStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, createdAt, updatedAt);
        this.riderId = riderId;
        this.rideType = rideType;
        this.origin = origin;
        this.destination = destination;
        this.status = status;
    }

    public UUID riderId() {
        return riderId;
    }

    public RideType rideType() {
        return rideType;
    }

    public LocationPoint origin() {
        return origin;
    }

    public LocationPoint destination() {
        return destination;
    }

    public RideStatus status() {
        return status;
    }

    public void markSearchingDriver(Instant timestamp) {
        this.status = RideStatus.SEARCHING_DRIVER;
        touch(timestamp);
    }
}
