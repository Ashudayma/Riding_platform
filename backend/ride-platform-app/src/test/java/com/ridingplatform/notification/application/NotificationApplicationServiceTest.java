package com.ridingplatform.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.id.IdGenerator;
import com.ridingplatform.config.NotificationProperties;
import com.ridingplatform.notification.domain.NotificationDispatchPlan;
import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.notification.domain.NotificationProviderResponse;
import com.ridingplatform.notification.domain.NotificationProviderStatus;
import com.ridingplatform.notification.domain.NotificationTemplateContent;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryAttemptEntity;
import com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryAttemptJpaRepository;
import com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryStatus;
import com.ridingplatform.notification.infrastructure.persistence.NotificationEntity;
import com.ridingplatform.notification.infrastructure.persistence.NotificationJpaRepository;
import com.ridingplatform.notification.infrastructure.persistence.NotificationType;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NotificationApplicationServiceTest {

    private final NotificationJpaRepository notificationJpaRepository = Mockito.mock(NotificationJpaRepository.class);
    private final NotificationDeliveryAttemptJpaRepository attemptJpaRepository = Mockito.mock(NotificationDeliveryAttemptJpaRepository.class);
    private final NotificationTemplateService templateService = Mockito.mock(NotificationTemplateService.class);
    private final UserNotificationPreferenceService preferenceService = Mockito.mock(UserNotificationPreferenceService.class);
    private final NotificationProviderRegistry providerRegistry = Mockito.mock(NotificationProviderRegistry.class);
    private final NotificationProvider provider = Mockito.mock(NotificationProvider.class);
    private final IdGenerator idGenerator = Mockito.mock(IdGenerator.class);
    private final EntityManager entityManager = Mockito.mock(EntityManager.class);

    private NotificationApplicationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationApplicationService(
                notificationJpaRepository,
                attemptJpaRepository,
                templateService,
                preferenceService,
                providerRegistry,
                new NotificationProperties(4, 30, 100, "en"),
                idGenerator,
                entityManager,
                Clock.fixed(Instant.parse("2026-04-18T12:00:00Z"), ZoneOffset.UTC),
                new ObjectMapper()
        );
        when(notificationJpaRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(attemptJpaRepository.save(any(NotificationDeliveryAttemptEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idGenerator.nextId()).thenReturn(
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                UUID.fromString("10000000-0000-0000-0000-000000000002"),
                UUID.fromString("10000000-0000-0000-0000-000000000003"),
                UUID.fromString("10000000-0000-0000-0000-000000000004")
        );
        when(entityManager.getReference(any(), any())).thenAnswer(invocation -> {
            Class<?> type = invocation.getArgument(0);
            UUID id = invocation.getArgument(1);
            if (type.getSimpleName().equals("UserProfileEntity")) {
                var entity = new com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity();
                entity.setId(id);
                return entity;
            }
            if (type.getSimpleName().equals("RideEntity")) {
                var entity = new com.ridingplatform.ride.infrastructure.persistence.RideEntity();
                entity.setId(id);
                return entity;
            }
            return null;
        });
        when(providerRegistry.provider(NotificationChannel.IN_APP)).thenReturn(provider);
        when(templateService.render(any(), any(), any(), any())).thenReturn(new NotificationTemplateContent("RIDE_BOOKED", "Ride booked", "Driver search started"));
    }

    @Test
    void shouldCreateAndDeliverNotificationWhenPreferenceEnabled() {
        when(preferenceService.isEnabled(any(), any(), any())).thenReturn(true);
        when(provider.send(any())).thenReturn(new NotificationProviderResponse(
                NotificationProviderStatus.DELIVERED,
                "in-app",
                "provider-ref",
                "{\"ok\":true}",
                null
        ));

        service.dispatch(List.of(new NotificationDispatchPlan(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                NotificationType.RIDE_UPDATE,
                NotificationEventCode.RIDE_BOOKED,
                NotificationChannel.IN_APP,
                "en",
                Map.of("rideCode", "RIDE-1")
        )));

        verify(notificationJpaRepository, times(2)).save(any(NotificationEntity.class));
        verify(attemptJpaRepository).save(any(NotificationDeliveryAttemptEntity.class));
    }

    @Test
    void shouldSkipDispatchWhenPreferenceDisabled() {
        when(preferenceService.isEnabled(any(), any(), any())).thenReturn(false);

        service.dispatch(List.of(new NotificationDispatchPlan(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                null,
                NotificationType.RIDE_UPDATE,
                NotificationEventCode.RIDE_BOOKED,
                NotificationChannel.IN_APP,
                "en",
                Map.of("rideCode", "RIDE-1")
        )));

        verify(notificationJpaRepository, times(0)).save(any(NotificationEntity.class));
    }

    @Test
    void shouldScheduleRetryWhenProviderFails() {
        when(preferenceService.isEnabled(any(), any(), any())).thenReturn(true);
        when(provider.send(any())).thenReturn(new NotificationProviderResponse(
                NotificationProviderStatus.FAILED,
                "in-app",
                null,
                "{\"ok\":false}",
                "temporary failure"
        ));

        service.dispatch(List.of(new NotificationDispatchPlan(
                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                null,
                NotificationType.SECURITY,
                NotificationEventCode.ACCOUNT_BLOCKED,
                NotificationChannel.IN_APP,
                "en",
                Map.of("rideCode", "RIDE-1")
        )));

        verify(notificationJpaRepository, times(2)).save(any(NotificationEntity.class));
    }
}
