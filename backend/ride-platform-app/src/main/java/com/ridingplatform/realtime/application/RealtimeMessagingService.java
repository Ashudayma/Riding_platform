package com.ridingplatform.realtime.application;

public interface RealtimeMessagingService {

    void sendToUser(String subject, String destination, Object payload);
}
