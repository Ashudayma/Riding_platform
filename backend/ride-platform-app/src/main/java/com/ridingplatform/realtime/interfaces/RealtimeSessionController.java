package com.ridingplatform.realtime.interfaces;

import com.ridingplatform.realtime.application.WebSocketSessionRegistry;
import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class RealtimeSessionController {

    private final WebSocketSessionRegistry webSocketSessionRegistry;

    public RealtimeSessionController(WebSocketSessionRegistry webSocketSessionRegistry) {
        this.webSocketSessionRegistry = webSocketSessionRegistry;
    }

    @MessageMapping("/session/ping")
    @SendToUser("/queue/session-state")
    public Map<String, Object> ping(@Header("simpSessionId") String sessionId, Principal principal) {
        webSocketSessionRegistry.heartbeat(sessionId);
        return Map.of(
                "state", "ALIVE",
                "sessionId", sessionId,
                "subject", principal == null ? "unknown" : principal.getName(),
                "occurredAt", Instant.now().toString()
        );
    }
}
