package com.ridingplatform.driver.application;

public interface DriverLocationEventPublisher {

    void publish(DriverLocationEvent event);
}
