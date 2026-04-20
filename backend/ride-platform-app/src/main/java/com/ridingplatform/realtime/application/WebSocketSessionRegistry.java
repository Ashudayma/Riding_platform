package com.ridingplatform.realtime.application;

import com.ridingplatform.config.ApplicationProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionRegistry {

    private final ApplicationProperties applicationProperties;
    private final RealtimeMessagingService realtimeMessagingService;
    private final Clock clock;
    private final Map<String, SessionSnapshot> sessions = new ConcurrentHashMap<>();

    public WebSocketSessionRegistry(
            ApplicationProperties applicationProperties,
            RealtimeMessagingService realtimeMessagingService,
            Clock clock
    ) {
        this.applicationProperties = applicationProperties;
        this.realtimeMessagingService = realtimeMessagingService;
        this.clock = clock;
    }

    public void connected(String sessionId, String subject) {
        sessions.put(sessionId, new SessionSnapshot(subject, Instant.now(clock)));
        realtimeMessagingService.sendToUser(subject, RealtimeDestination.SESSION_STATE_QUEUE,
                new SessionStatePayload("CONNECTED", sessionId, Instant.now(clock)));
    }

    public void heartbeat(String sessionId) {
        sessions.computeIfPresent(sessionId, (key, snapshot) -> new SessionSnapshot(snapshot.subject(), Instant.now(clock)));
    }

    public void disconnected(String sessionId) {
        SessionSnapshot snapshot = sessions.remove(sessionId);
        if (snapshot != null) {
            realtimeMessagingService.sendToUser(snapshot.subject(), RealtimeDestination.SESSION_STATE_QUEUE,
                    new SessionStatePayload("DISCONNECTED", sessionId, Instant.now(clock)));
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void evictStaleSessions() {
        Instant cutoff = Instant.now(clock).minusSeconds(applicationProperties.realtime().staleSessionSeconds());
        sessions.entrySet().removeIf(entry -> {
            boolean stale = entry.getValue().lastSeenAt().isBefore(cutoff);
            if (stale) {
                realtimeMessagingService.sendToUser(entry.getValue().subject(), RealtimeDestination.SESSION_STATE_QUEUE,
                        new SessionStatePayload("STALE", entry.getKey(), Instant.now(clock)));
            }
            return stale;
        });
    }

    private record SessionSnapshot(String subject, Instant lastSeenAt) {
    }
}
