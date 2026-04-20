package com.ridingplatform.common.messaging;

import com.ridingplatform.common.domain.DomainEvent;

public interface EventPublisher {

    void publish(DomainEvent event);
}
