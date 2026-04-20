package com.ridingplatform.realtime.infrastructure;

import com.ridingplatform.realtime.application.RealtimeMessagingService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompRealtimeMessagingService implements RealtimeMessagingService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public StompRealtimeMessagingService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Override
    public void sendToUser(String subject, String destination, Object payload) {
        if (subject == null || subject.isBlank()) {
            return;
        }
        simpMessagingTemplate.convertAndSendToUser(subject, destination, payload);
    }
}
