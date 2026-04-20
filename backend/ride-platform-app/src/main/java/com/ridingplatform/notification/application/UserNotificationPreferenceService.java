package com.ridingplatform.notification.application;

import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.notification.domain.NotificationPreferenceView;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.UserNotificationPreferenceEntity;
import com.ridingplatform.notification.infrastructure.persistence.UserNotificationPreferenceJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserNotificationPreferenceService {

    private static final Map<NotificationEventCode, List<NotificationChannel>> DEFAULT_CHANNELS = Map.of(
            NotificationEventCode.RIDE_BOOKED, List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
            NotificationEventCode.DRIVER_ASSIGNED_RIDER, List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
            NotificationEventCode.DRIVER_ASSIGNED_DRIVER, List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
            NotificationEventCode.RIDE_CANCELLED, List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH),
            NotificationEventCode.PAYMENT_FAILED, List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
            NotificationEventCode.PAYMENT_CAPTURED, List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL),
            NotificationEventCode.ACCOUNT_BLOCKED, List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL, NotificationChannel.SMS),
            NotificationEventCode.FRAUD_ALERT_ANALYST, List.of(NotificationChannel.IN_APP)
    );

    private final UserNotificationPreferenceJpaRepository repository;
    private final IdGenerator idGenerator;
    private final EntityManager entityManager;

    public UserNotificationPreferenceService(
            UserNotificationPreferenceJpaRepository repository,
            IdGenerator idGenerator,
            EntityManager entityManager
    ) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userProfileId, NotificationEventCode eventCode, NotificationChannel channel) {
        return repository.findByUserProfileIdAndEventCodeAndChannelAndDeletedAtIsNull(userProfileId, eventCode.name(), channel)
                .map(UserNotificationPreferenceEntity::isEnabled)
                .orElse(DEFAULT_CHANNELS.getOrDefault(eventCode, List.of(NotificationChannel.IN_APP)).contains(channel));
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceView> list(UUID userProfileId) {
        return repository.findByUserProfileIdAndDeletedAtIsNullOrderByEventCodeAsc(userProfileId).stream()
                .map(entity -> new NotificationPreferenceView(
                        entity.getId(),
                        NotificationEventCode.valueOf(entity.getEventCode()),
                        entity.getChannel(),
                        entity.isEnabled()
                ))
                .toList();
    }

    @Transactional
    public NotificationPreferenceView upsert(UUID userProfileId, NotificationEventCode eventCode, NotificationChannel channel, boolean enabled) {
        UserNotificationPreferenceEntity entity = repository.findByUserProfileIdAndEventCodeAndChannelAndDeletedAtIsNull(userProfileId, eventCode.name(), channel)
                .orElseGet(() -> {
                    UserNotificationPreferenceEntity created = new UserNotificationPreferenceEntity();
                    created.setId(idGenerator.nextId());
                    created.setUserProfile(entityManager.getReference(UserProfileEntity.class, userProfileId));
                    created.setEventCode(eventCode.name());
                    created.setChannel(channel);
                    return created;
                });
        entity.setEnabled(enabled);
        repository.save(entity);
        return new NotificationPreferenceView(entity.getId(), eventCode, channel, enabled);
    }
}
