package com.ridingplatform.common.id;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UuidGenerator implements IdGenerator {

    @Override
    public UUID nextId() {
        return UUID.randomUUID();
    }
}
