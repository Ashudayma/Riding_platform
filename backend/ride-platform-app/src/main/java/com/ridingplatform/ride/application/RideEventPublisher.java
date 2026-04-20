package com.ridingplatform.ride.application;

import java.util.UUID;

public interface RideEventPublisher {

    void publish(RideEventType eventType, UUID aggregateId, Object payload);
}
