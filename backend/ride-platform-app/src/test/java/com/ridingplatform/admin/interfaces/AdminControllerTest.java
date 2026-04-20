package com.ridingplatform.admin.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.admin.application.AdminActionRequest;
import com.ridingplatform.admin.application.AdminApplicationService;
import com.ridingplatform.admin.application.AdminOperationalMetricsView;
import com.ridingplatform.admin.application.AdminPage;
import com.ridingplatform.admin.application.AdminProfileView;
import com.ridingplatform.admin.application.AdminRideView;
import com.ridingplatform.common.web.GlobalExceptionHandler;
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
class AdminControllerTest {

    @Mock
    private AdminApplicationService adminApplicationService;

    @Mock
    private SecurityContextFacade securityContextFacade;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminController(adminApplicationService, securityContextFacade))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnOverview() throws Exception {
        when(adminApplicationService.operationalMetrics()).thenReturn(new AdminOperationalMetricsView(3, 4, 10, 1, 2, 5, 6));

        mockMvc.perform(get("/api/v1/admin/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ridesInProgress").value(3))
                .andExpect(jsonPath("$.availableDrivers").value(10));
    }

    @Test
    void shouldReturnPagedRides() throws Exception {
        when(adminApplicationService.searchRides(any(), any(), any(), any(), any(), any(), any(), any(Integer.class), any(Integer.class), any()))
                .thenReturn(new AdminPage<>(
                        List.of(new AdminRideView(
                                UUID.fromString("20000000-0000-0000-0000-000000000001"),
                                UUID.fromString("21000000-0000-0000-0000-000000000001"),
                                UUID.fromString("22000000-0000-0000-0000-000000000001"),
                                null,
                                "STANDARD",
                                "SEARCHING_DRIVER",
                                "SEARCHING_DRIVER",
                                "Pickup",
                                "Drop",
                                Instant.parse("2026-04-18T12:00:00Z"),
                                null,
                                null
                        )),
                        1, 0, 20, 1
                ));

        mockMvc.perform(get("/api/v1/admin/rides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].requestStatus").value("SEARCHING_DRIVER"));
    }

    @Test
    void shouldBlockDriver() throws Exception {
        when(securityContextFacade.currentActor()).thenReturn(Optional.of(
                new CurrentActor("admin", UUID.fromString("10000000-0000-0000-0000-000000000001"), "admin", Set.of("OPS_ADMIN"))
        ));
        when(adminApplicationService.blockDriver(any(), any(), any(), any(), any(), any(), any())).thenReturn(
                new AdminProfileView(
                        UUID.fromString("40000000-0000-0000-0000-000000000001"),
                        UUID.fromString("41000000-0000-0000-0000-000000000001"),
                        "DRIVER-1",
                        "BLOCKED",
                        new BigDecimal("4.90"),
                        new BigDecimal("20.00"),
                        true,
                        false,
                        true,
                        "Driver One",
                        "driver@example.com"
                )
        );

        mockMvc.perform(patch("/api/v1/admin/drivers/{driverProfileId}/block", "40000000-0000-0000-0000-000000000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminActionRequest("ops action"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }
}
