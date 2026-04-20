package com.ridingplatform.fraud.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.web.GlobalExceptionHandler;
import com.ridingplatform.fraud.application.FraudRiskService;
import com.ridingplatform.fraud.domain.FraudProfileSummary;
import com.ridingplatform.fraud.domain.RiskLevel;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSignalEventEntity;
import com.ridingplatform.fraud.infrastructure.persistence.FraudSubjectType;
import com.ridingplatform.security.application.AdminAuditService;
import com.ridingplatform.security.application.CurrentActor;
import com.ridingplatform.security.application.SecurityContextFacade;
import java.math.BigDecimal;
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
class FraudOperationsControllerTest {

    @Mock
    private FraudRiskService fraudRiskService;

    @Mock
    private SecurityContextFacade securityContextFacade;

    @Mock
    private AdminAuditService adminAuditService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        FraudOperationsController controller = new FraudOperationsController(fraudRiskService, securityContextFacade, adminAuditService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnProfileWithSignals() throws Exception {
        UUID subjectId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        when(fraudRiskService.getProfile(FraudSubjectType.RIDER, subjectId)).thenReturn(new FraudProfileSummary(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                FraudSubjectType.RIDER,
                subjectId,
                new BigDecimal("22.0000"),
                RiskLevel.MEDIUM,
                1,
                false,
                false,
                false,
                null,
                Instant.parse("2026-04-18T12:00:00Z"),
                Instant.parse("2026-04-18T12:00:00Z")
        ));
        FraudSignalEventEntity signal = new FraudSignalEventEntity();
        signal.setId(UUID.fromString("70000000-0000-0000-0000-000000000001"));
        signal.setSignalType("RIDE_CANCELLED");
        signal.setOccurredAt(Instant.parse("2026-04-18T12:00:00Z"));
        signal.setSourceTopic("riding-platform.ride.cancelled");
        signal.setTriggeredRulesJson("[\"REPEATED_CANCELLATIONS\"]");
        when(fraudRiskService.recentSignals(FraudSubjectType.RIDER, subjectId)).thenReturn(List.of(signal));

        mockMvc.perform(get("/api/v1/fraud/profiles/RIDER/{subjectId}", subjectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.subjectType").value("RIDER"))
                .andExpect(jsonPath("$.recentSignals[0].signalType").value("RIDE_CANCELLED"));
    }

    @Test
    void shouldReviewFlag() throws Exception {
        UUID flagId = UUID.fromString("80000000-0000-0000-0000-000000000001");
        UUID actorId = UUID.fromString("90000000-0000-0000-0000-000000000001");
        when(securityContextFacade.currentActor()).thenReturn(Optional.of(new CurrentActor("user-1", actorId, "analyst", Set.of("FRAUD_ANALYST"))));
        when(fraudRiskService.reviewFlag(any(), any(), any(), any(), any())).thenReturn(new FraudProfileSummary(
                UUID.fromString("50000000-0000-0000-0000-000000000001"),
                FraudSubjectType.DRIVER,
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                new BigDecimal("82.0000"),
                RiskLevel.CRITICAL,
                2,
                true,
                true,
                true,
                "Manual fraud review block",
                Instant.parse("2026-04-18T12:00:00Z"),
                Instant.parse("2026-04-18T12:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/fraud/flags/{flagId}/review", flagId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FraudFlagReviewHttpRequest(
                                FraudFlagStatus.CONFIRMED,
                                "Escalating after analyst confirmation",
                                true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true))
                .andExpect(jsonPath("$.riskLevel").value("CRITICAL"));
    }
}
