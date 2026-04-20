package com.ridingplatform.common.messaging;

import com.ridingplatform.common.domain.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publish(DomainEvent event) {
        log.info("Publishing domain event type={} eventId={}", event.eventType(), event.eventId());
    }
}
