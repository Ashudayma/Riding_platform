package com.ridingplatform.notification.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.config.NotificationProperties;
import com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity;
import com.ridingplatform.notification.domain.NotificationDispatchPlan;
import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.notification.domain.NotificationPreferenceView;
import com.ridingplatform.notification.domain.NotificationProviderResponse;
import com.ridingplatform.notification.domain.NotificationProviderStatus;
import com.ridingplatform.notification.domain.NotificationTemplateContent;
import com.ridingplatform.notification.domain.NotificationView;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryAttemptEntity;
import com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryAttemptJpaRepository;
import com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryStatus;
import com.ridingplatform.notification.infrastructure.persistence.NotificationEntity;
import com.ridingplatform.notification.infrastructure.persistence.NotificationJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationApplicationService {

    private final NotificationJpaRepository notificationJpaRepository;
    private final NotificationDeliveryAttemptJpaRepository notificationDeliveryAttemptJpaRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final UserNotificationPreferenceService preferenceService;
    private final NotificationProviderRegistry providerRegistry;
    private final NotificationProperties notificationProperties;
    private final IdGenerator idGenerator;
    private final EntityManager entityManager;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public NotificationApplicationService(
            NotificationJpaRepository notificationJpaRepository,
            NotificationDeliveryAttemptJpaRepository notificationDeliveryAttemptJpaRepository,
            NotificationTemplateService notificationTemplateService,
            UserNotificationPreferenceService preferenceService,
            NotificationProviderRegistry providerRegistry,
            NotificationProperties notificationProperties,
            IdGenerator idGenerator,
            EntityManager entityManager,
            Clock clock,
            ObjectMapper objectMapper
    ) {
        this.notificationJpaRepository = notificationJpaRepository;
        this.notificationDeliveryAttemptJpaRepository = notificationDeliveryAttemptJpaRepository;
        this.notificationTemplateService = notificationTemplateService;
        this.preferenceService = preferenceService;
        this.providerRegistry = providerRegistry;
        this.notificationProperties = notificationProperties;
        this.idGenerator = idGenerator;
        this.entityManager = entityManager;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void dispatch(List<NotificationDispatchPlan> plans) {
        for (NotificationDispatchPlan plan : plans) {
            if (!preferenceService.isEnabled(plan.userProfileId(), plan.eventCode(), plan.channel())) {
                continue;
            }
            NotificationTemplateContent content = notificationTemplateService.render(
                    plan.eventCode().name(),
                    plan.channel(),
                    plan.locale(),
                    plan.templateModel()
            );
            NotificationEntity entity = new NotificationEntity();
            entity.setId(idGenerator.nextId());
            entity.setUserProfile(entityManager.getReference(UserProfileEntity.class, plan.userProfileId()));
            if (plan.rideId() != null) {
                entity.setRide(entityManager.getReference(com.ridingplatform.ride.infrastructure.persistence.RideEntity.class, plan.rideId()));
            }
            entity.setNotificationType(plan.notificationType());
            entity.setEventCode(plan.eventCode());
            entity.setChannel(plan.channel());
            entity.setDeliveryStatus(NotificationDeliveryStatus.PENDING);
            entity.setTemplateKey(content.templateKey());
            entity.setTitle(content.title());
            entity.setBody(content.body());
            entity.setPayloadJson(writeJson(plan.templateModel()));
            entity.setScheduledAt(Instant.now(clock));
            notificationJpaRepository.save(entity);
            attemptSend(entity);
        }
    }

    @Transactional
    public void retryDueNotifications() {
        notificationJpaRepository.findRetryable(notificationProperties.maxRetryAttempts()).stream()
                .limit(notificationProperties.retryScanBatchSize())
                .forEach(this::attemptSend);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> listForUser(UUID userProfileId) {
        return notificationJpaRepository.findRecentForUser(userProfileId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public NotificationView markRead(UUID notificationId, UUID userProfileId) {
        NotificationEntity entity = notificationJpaRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));
        if (!entity.getUserProfile().getId().equals(userProfileId)) {
            throw new NotificationNotFoundException("Notification not found");
        }
        entity.setDeliveryStatus(NotificationDeliveryStatus.READ);
        entity.setReadAt(Instant.now(clock));
        notificationJpaRepository.save(entity);
        return toView(entity);
    }

    @Transactional(readOnly = true)
    public List<NotificationPreferenceView> listPreferences(UUID userProfileId) {
        return preferenceService.list(userProfileId);
    }

    @Transactional
    public NotificationPreferenceView updatePreference(UUID userProfileId, NotificationEventCode eventCode, NotificationChannel channel, boolean enabled) {
        return preferenceService.upsert(userProfileId, eventCode, channel, enabled);
    }

    private void attemptSend(NotificationEntity entity) {
        Instant now = Instant.now(clock);
        entity.setLastAttemptAt(now);
        NotificationProvider provider = providerRegistry.provider(entity.getChannel());
        NotificationProviderResponse response = provider == null
                ? new NotificationProviderResponse(NotificationProviderStatus.FAILED, null, null, null, "No provider configured for channel " + entity.getChannel())
                : provider.send(entity);
        applyResponse(entity, response, now);
        notificationJpaRepository.save(entity);
        saveAttempt(entity, response, now);
    }

    private void applyResponse(NotificationEntity entity, NotificationProviderResponse response, Instant now) {
        entity.setProviderKey(response.providerKey());
        entity.setProviderMessageRef(response.providerMessageRef());
        entity.setFailureReason(response.failureReason());
        switch (response.status()) {
            case SENT -> {
                entity.setDeliveryStatus(NotificationDeliveryStatus.SENT);
                entity.setSentAt(now);
                entity.setNextRetryAt(null);
            }
            case DELIVERED -> {
                entity.setDeliveryStatus(NotificationDeliveryStatus.DELIVERED);
                entity.setSentAt(now);
                entity.setDeliveredAt(now);
                entity.setNextRetryAt(null);
            }
            case SKIPPED -> {
                entity.setDeliveryStatus(NotificationDeliveryStatus.CANCELLED);
                entity.setNextRetryAt(null);
            }
            case FAILED, RETRY_SCHEDULED -> {
                entity.setRetryCount(entity.getRetryCount() + 1);
                entity.setDeliveryStatus(NotificationDeliveryStatus.FAILED);
                if (entity.getRetryCount() < notificationProperties.maxRetryAttempts()) {
                    long delay = (long) notificationProperties.initialRetryDelaySeconds() * (1L << Math.max(0, entity.getRetryCount() - 1));
                    entity.setNextRetryAt(now.plusSeconds(delay));
                } else {
                    entity.setNextRetryAt(null);
                }
            }
        }
    }

    private void saveAttempt(NotificationEntity entity, NotificationProviderResponse response, Instant now) {
        NotificationDeliveryAttemptEntity attempt = new NotificationDeliveryAttemptEntity();
        attempt.setId(idGenerator.nextId());
        attempt.setNotification(entity);
        attempt.setAttemptNo(entity.getRetryCount() == 0 ? 1 : entity.getRetryCount());
        attempt.setProviderKey(response.providerKey());
        attempt.setProviderStatus(response.status());
        attempt.setResponsePayloadJson(response.responsePayloadJson());
        attempt.setFailureReason(response.failureReason());
        attempt.setAttemptedAt(now);
        attempt.setCompletedAt(now);
        notificationDeliveryAttemptJpaRepository.save(attempt);
    }

    private NotificationView toView(NotificationEntity entity) {
        return new NotificationView(
                entity.getId(),
                entity.getRide() == null ? null : entity.getRide().getId(),
                entity.getNotificationType(),
                entity.getEventCode(),
                entity.getChannel(),
                entity.getDeliveryStatus(),
                entity.getTitle(),
                entity.getBody(),
                entity.getSentAt(),
                entity.getDeliveredAt(),
                entity.getReadAt(),
                entity.getFailureReason()
        );
    }

    private String writeJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }
}
