package com.ridingplatform.config;

import com.ridingplatform.realtime.infrastructure.StompPrincipalChannelInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final ApplicationProperties applicationProperties;
    private final StompPrincipalChannelInterceptor stompPrincipalChannelInterceptor;

    public WebSocketConfiguration(
            ApplicationProperties applicationProperties,
            StompPrincipalChannelInterceptor stompPrincipalChannelInterceptor
    ) {
        this.applicationProperties = applicationProperties;
        this.stompPrincipalChannelInterceptor = stompPrincipalChannelInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{
                        applicationProperties.realtime().brokerHeartbeatMillis(),
                        applicationProperties.realtime().brokerHeartbeatMillis()
                })
                .setTaskScheduler(webSocketBrokerTaskScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompPrincipalChannelInterceptor);
    }

    @Override
    public boolean configureMessageConverters(java.util.List<org.springframework.messaging.converter.MessageConverter> messageConverters) {
        return false;
    }

    @Bean
    public TaskScheduler webSocketBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("ws-broker-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxBinaryMessageBufferSize(64 * 1024);
        container.setMaxSessionIdleTimeout(applicationProperties.realtime().staleSessionSeconds() * 1000L);
        return container;
    }
}
