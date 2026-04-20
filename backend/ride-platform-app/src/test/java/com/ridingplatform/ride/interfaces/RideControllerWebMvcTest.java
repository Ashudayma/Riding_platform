package com.ridingplatform.ride.interfaces;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridingplatform.common.web.GlobalExceptionHandler;
import com.ridingplatform.config.SecurityProperties;
import com.ridingplatform.ride.application.FareEstimateResult;
import com.ridingplatform.ride.application.RideApplicationService;
import com.ridingplatform.ride.application.RideBookingResult;
import com.ridingplatform.ride.application.RideBookingStatus;
import com.ridingplatform.ride.domain.RideType;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import com.ridingplatform.security.application.CurrentActor;
import com.ridingplatform.security.application.SecurityContextFacade;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = RideController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, RideControllerWebMvcTest.TestSecurityConfig.class})
class RideControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RideApplicationService rideApplicationService;

    @MockBean
    private SecurityContextFacade securityContextFacade;

    @MockBean
    private RiderProfileJpaRepository riderProfileJpaRepository;

    @MockBean
    private SecurityProperties securityProperties;

    @MockBean
    private com.ridingplatform.security.application.AdminAuditService adminAuditService;

    @MockBean
    private com.ridingplatform.security.web.CorrelationIdFilter correlationIdFilter;

    @MockBean
    private com.ridingplatform.security.web.SecureRequestLoggingFilter secureRequestLoggingFilter;

    @MockBean
    private com.ridingplatform.security.web.RateLimitingFilter rateLimitingFilter;

    @MockBean
    private com.ridingplatform.security.web.IdempotencyFilter idempotencyFilter;

    @MockBean
    private com.ridingplatform.security.web.ErrorResponseWriter errorResponseWriter;

    @BeforeEach
    void setUp() {
        when(securityProperties.audit()).thenReturn(new SecurityProperties.Audit(false, List.of(), false));
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http.csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }

    @Test
    @WithMockUser(roles = "RIDER")
    void shouldEstimateFareForAuthenticatedRider() throws Exception {
        UUID userProfileId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID riderProfileId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        RiderProfileEntity riderProfile = new RiderProfileEntity();
        riderProfile.setId(riderProfileId);
        when(securityContextFacade.currentActor()).thenReturn(Optional.of(new CurrentActor("sub", userProfileId, "rider", java.util.Set.of("RIDER"))));
        when(riderProfileJpaRepository.findByUserProfileId(userProfileId)).thenReturn(Optional.of(riderProfile));
        when(rideApplicationService.estimateFare(any())).thenReturn(new FareEstimateResult(
                UUID.fromString("80000000-0000-0000-0000-000000000001"),
                RideType.SHARED,
                new BigDecimal("70.00"),
                new BigDecimal("90.00"),
                new BigDecimal("20.00"),
                new BigDecimal("1.00"),
                new BigDecimal("15.00"),
                new BigDecimal("9.50"),
                BigDecimal.ZERO,
                new BigDecimal("25.00"),
                new BigDecimal("179.50"),
                "INR",
                12000,
                1800,
                Instant.parse("2026-04-18T10:10:00Z")
        ));

        mockMvc.perform(post("/api/v1/rides/estimate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FareEstimateHttpRequest(
                                RideType.SHARED, (short) 1, null, 28.6139, 77.2090, "Pickup",
                                28.4595, 77.0266, "Drop", List.of()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyCode").value("INR"))
                .andExpect(jsonPath("$.rideType").value("SHARED"));
    }

    @Test
    @WithMockUser(roles = "RIDER")
    void shouldReturnRideHistory() throws Exception {
        UUID userProfileId = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID riderProfileId = UUID.fromString("30000000-0000-0000-0000-000000000001");
        RiderProfileEntity riderProfile = new RiderProfileEntity();
        riderProfile.setId(riderProfileId);
        when(securityContextFacade.currentActor()).thenReturn(Optional.of(new CurrentActor("sub", userProfileId, "rider", java.util.Set.of("RIDER"))));
        when(riderProfileJpaRepository.findByUserProfileId(userProfileId)).thenReturn(Optional.of(riderProfile));
        when(rideApplicationService.getRideHistory(riderProfileId)).thenReturn(List.of(
                new com.ridingplatform.ride.application.RideHistoryItemResult(
                        UUID.fromString("90000000-0000-0000-0000-000000000001"),
                        UUID.fromString("93000000-0000-0000-0000-000000000001"),
                        RideType.STANDARD,
                        RideBookingStatus.COMPLETED,
                        "Pickup",
                        "Drop",
                        new BigDecimal("220.00"),
                        "INR",
                        Instant.parse("2026-04-18T10:00:00Z")
                )
        ));

        mockMvc.perform(get("/api/v1/rides/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
