package com.ridingplatform.realtime.infrastructure;

import com.ridingplatform.realtime.application.WebSocketSessionRegistry;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class StompPrincipalChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter;
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    public StompPrincipalChannelInterceptor(
            JwtDecoder jwtDecoder,
            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter,
            WebSocketSessionRegistry webSocketSessionRegistry
    ) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.webSocketSessionRegistry = webSocketSessionRegistry;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = bearerToken(accessor);
            Jwt jwt = jwtDecoder.decode(token);
            AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
            if (authentication == null) {
                throw new IllegalStateException("Unable to authenticate websocket session");
            }
            authentication.setAuthenticated(true);
            accessor.setUser(authentication);
            webSocketSessionRegistry.connected(accessor.getSessionId(), authentication.getName());
        } else if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new IllegalStateException("Websocket session is not authenticated");
            }
            webSocketSessionRegistry.heartbeat(accessor.getSessionId());
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            webSocketSessionRegistry.disconnected(accessor.getSessionId());
        }
        return message;
    }

    private String bearerToken(StompHeaderAccessor accessor) {
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization == null || authorization.isEmpty()) {
            throw new IllegalStateException("Missing Authorization header for websocket connect");
        }
        String header = authorization.get(0);
        if (!header.startsWith("Bearer ")) {
            throw new IllegalStateException("Invalid Authorization header for websocket connect");
        }
        return header.substring(7);
    }
}
