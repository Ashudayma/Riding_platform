package com.ridingplatform.driver.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.web.GlobalExceptionHandler;
import com.ridingplatform.driver.application.DriverAvailabilityResult;
import com.ridingplatform.driver.application.DriverAvailabilityService;
import com.ridingplatform.driver.application.DriverLocationUpdateResult;
import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.OnlineStatus;
import com.ridingplatform.security.application.SecurityContextFacade;
import com.ridingplatform.tracking.infrastructure.persistence.LocationProviderType;
import java.math.BigDecimal;
import java.time.Instant;
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
class DriverOperationsControllerTest {

    @Mock
    private SecurityContextFacade securityContextFacade;

    @Mock
    private DriverAvailabilityService driverAvailabilityService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        DriverOperationsController controller = new DriverOperationsController(securityContextFacade, driverAvailabilityService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldUpdateAvailability() throws Exception {
        when(driverAvailabilityService.updateAvailability(any())).thenReturn(new DriverAvailabilityResult(
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                AvailabilityStatus.AVAILABLE,
                OnlineStatus.ONLINE,
                (short) 4,
                null,
                0,
                0,
                new BigDecimal("5"),
                Instant.parse("2026-04-18T12:00:00Z")
        ));

        mockMvc.perform(patch("/api/v1/driver/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DriverAvailabilityRequest(
                                true, (short) 4, "session-1", "1.0.0", "ANDROID"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.onlineStatus").value("ONLINE"))
                .andExpect(jsonPath("$.availabilityStatus").value("AVAILABLE"));
    }

    @Test
    void shouldUpdateLocation() throws Exception {
        when(driverAvailabilityService.updateLiveLocation(any())).thenReturn(new DriverLocationUpdateResult(
                UUID.fromString("40000000-0000-0000-0000-000000000001"),
                28.6139,
                77.2090,
                Instant.parse("2026-04-18T12:00:00Z")
        ));

        mockMvc.perform(post("/api/v1/driver/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DriverLocationUpdateRequest(
                                28.6139,
                                77.2090,
                                new BigDecimal("180"),
                                new BigDecimal("32"),
                                new BigDecimal("5"),
                                LocationProviderType.GPS
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(28.6139))
                .andExpect(jsonPath("$.longitude").value(77.2090));
    }
}
