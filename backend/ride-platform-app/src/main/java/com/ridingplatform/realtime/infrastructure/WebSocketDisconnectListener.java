package com.ridingplatform.realtime.infrastructure;

import com.ridingplatform.realtime.application.WebSocketSessionRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketDisconnectListener {

    private final WebSocketSessionRegistry webSocketSessionRegistry;

    public WebSocketDisconnectListener(WebSocketSessionRegistry webSocketSessionRegistry) {
        this.webSocketSessionRegistry = webSocketSessionRegistry;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getSessionId() != null) {
            webSocketSessionRegistry.disconnected(accessor.getSessionId());
        }
    }
}
