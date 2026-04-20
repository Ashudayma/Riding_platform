package com.ridingplatform.notification.application;

import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationProviderRegistry {

    private final Map<NotificationChannel, NotificationProvider> providers = new EnumMap<>(NotificationChannel.class);

    public NotificationProviderRegistry(List<NotificationProvider> providerList) {
        for (NotificationProvider provider : providerList) {
            providers.put(provider.channel(), provider);
        }
    }

    public NotificationProvider provider(NotificationChannel channel) {
        return providers.get(channel);
    }
}
