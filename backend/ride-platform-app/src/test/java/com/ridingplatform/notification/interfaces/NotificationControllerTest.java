package com.ridingplatform.notification.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.web.GlobalExceptionHandler;
import com.ridingplatform.notification.application.NotificationApplicationService;
import com.ridingplatform.notification.domain.NotificationEventCode;
import com.ridingplatform.notification.domain.NotificationPreferenceView;
import com.ridingplatform.notification.domain.NotificationView;
import com.ridingplatform.notification.infrastructure.persistence.NotificationChannel;
import com.ridingplatform.notification.infrastructure.persistence.NotificationDeliveryStatus;
import com.ridingplatform.notification.infrastructure.persistence.NotificationType;
import com.ridingplatform.security.application.CurrentActor;
import com.ridingplatform.security.application.SecurityContextFacade;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationApplicationService notificationApplicationService;

    @Mock
    private SecurityContextFacade securityContextFacade;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        NotificationController controller = new NotificationController(notificationApplicationService, securityContextFacade);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(securityContextFacade.currentActor()).thenReturn(Optional.of(
                new CurrentActor("user", UUID.fromString("20000000-0000-0000-0000-000000000001"), "user", Set.of("RIDER"))
        ));
    }

    @Test
    void shouldListNotifications() throws Exception {
        when(notificationApplicationService.listForUser(any())).thenReturn(List.of(
                new NotificationView(
                        UUID.fromString("30000000-0000-0000-0000-000000000001"),
                        UUID.fromString("40000000-0000-0000-0000-000000000001"),
                        NotificationType.RIDE_UPDATE,
                        NotificationEventCode.RIDE_BOOKED,
                        NotificationChannel.IN_APP,
                        NotificationDeliveryStatus.DELIVERED,
                        "Ride booked",
                        "Driver search started",
                        Instant.parse("2026-04-18T12:00:00Z"),
                        Instant.parse("2026-04-18T12:00:01Z"),
                        null,
                        null
                )
        ));

        mockMvc.perform(get("/api/v1/notifications/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("RIDE_BOOKED"));
    }

    @Test
    void shouldMarkNotificationRead() throws Exception {
        when(notificationApplicationService.markRead(any(), any())).thenReturn(new NotificationView(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                null,
                NotificationType.RIDE_UPDATE,
                NotificationEventCode.RIDE_BOOKED,
                NotificationChannel.IN_APP,
                NotificationDeliveryStatus.READ,
                "Ride booked",
                "Driver search started",
                Instant.parse("2026-04-18T12:00:00Z"),
                Instant.parse("2026-04-18T12:00:01Z"),
                Instant.parse("2026-04-18T12:02:00Z"),
                null
        ));

        mockMvc.perform(patch("/api/v1/notifications/{notificationId}/read", "30000000-0000-0000-0000-000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("READ"));
    }

    @Test
    void shouldUpdatePreference() throws Exception {
        when(notificationApplicationService.updatePreference(any(), any(), any(), any(Boolean.class))).thenReturn(
                new NotificationPreferenceView(
                        UUID.fromString("50000000-0000-0000-0000-000000000001"),
                        NotificationEventCode.PAYMENT_FAILED,
                        NotificationChannel.EMAIL,
                        true
                )
        );

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NotificationPreferenceHttpRequest(
                                NotificationEventCode.PAYMENT_FAILED,
                                NotificationChannel.EMAIL,
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("EMAIL"));
    }
}
