package com.ridingplatform.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ridingplatform.admin.infrastructure.persistence.AdminAuditLogJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverAvailabilityJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileJpaRepository;
import com.ridingplatform.driver.infrastructure.persistence.DriverStatus;
import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagJpaRepository;
import com.ridingplatform.fraud.infrastructure.persistence.FraudRiskProfileJpaRepository;
import com.ridingplatform.pricing.infrastructure.persistence.PricingRuleSetJpaRepository;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileJpaRepository;
import com.ridingplatform.rider.infrastructure.persistence.RiderStatus;
import com.ridingplatform.ride.infrastructure.SpringDataRideRequestJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.RideStatusHistoryJpaRepository;
import com.ridingplatform.security.application.AdminAuditService;
import com.ridingplatform.security.application.CurrentActor;
import com.ridingplatform.sharedride.infrastructure.persistence.SharedRideGroupJpaRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AdminApplicationServiceTest {

    private final SpringDataRideRequestJpaRepository rideRequestJpaRepository = Mockito.mock(SpringDataRideRequestJpaRepository.class);
    private final RideJpaRepository rideJpaRepository = Mockito.mock(RideJpaRepository.class);
    private final RiderProfileJpaRepository riderProfileJpaRepository = Mockito.mock(RiderProfileJpaRepository.class);
    private final DriverProfileJpaRepository driverProfileJpaRepository = Mockito.mock(DriverProfileJpaRepository.class);
    private final FraudFlagJpaRepository fraudFlagJpaRepository = Mockito.mock(FraudFlagJpaRepository.class);
    private final FraudRiskProfileJpaRepository fraudRiskProfileJpaRepository = Mockito.mock(FraudRiskProfileJpaRepository.class);
    private final RideStatusHistoryJpaRepository rideStatusHistoryJpaRepository = Mockito.mock(RideStatusHistoryJpaRepository.class);
    private final PricingRuleSetJpaRepository pricingRuleSetJpaRepository = Mockito.mock(PricingRuleSetJpaRepository.class);
    private final DriverAvailabilityJpaRepository driverAvailabilityJpaRepository = Mockito.mock(DriverAvailabilityJpaRepository.class);
    private final SharedRideGroupJpaRepository sharedRideGroupJpaRepository = Mockito.mock(SharedRideGroupJpaRepository.class);
    private final DriverAssignmentAttemptJpaRepository driverAssignmentAttemptJpaRepository = Mockito.mock(DriverAssignmentAttemptJpaRepository.class);
    private final AdminAuditService adminAuditService = Mockito.mock(AdminAuditService.class);
    private final AdminAuditLogJpaRepository adminAuditLogJpaRepository = Mockito.mock(AdminAuditLogJpaRepository.class);

    private AdminApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AdminApplicationService(
                rideRequestJpaRepository,
                rideJpaRepository,
                riderProfileJpaRepository,
                driverProfileJpaRepository,
                fraudFlagJpaRepository,
                fraudRiskProfileJpaRepository,
                rideStatusHistoryJpaRepository,
                pricingRuleSetJpaRepository,
                driverAvailabilityJpaRepository,
                sharedRideGroupJpaRepository,
                driverAssignmentAttemptJpaRepository,
                adminAuditService,
                adminAuditLogJpaRepository
        );
    }

    @Test
    void shouldBlockRiderAndAuditAction() {
        RiderProfileEntity rider = new RiderProfileEntity();
        rider.setId(UUID.fromString("30000000-0000-0000-0000-000000000001"));
        rider.setRiderStatus(RiderStatus.ACTIVE);
        rider.setAverageRating(new BigDecimal("4.80"));
        rider.setRiderCode("RIDER-1");
        var user = new com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity();
        user.setId(UUID.fromString("31000000-0000-0000-0000-000000000001"));
        user.setDisplayName("Rider One");
        user.setEmail("rider@example.com");
        rider.setUserProfile(user);
        when(riderProfileJpaRepository.findById(rider.getId())).thenReturn(Optional.of(rider));
        when(riderProfileJpaRepository.save(any(RiderProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminProfileView view = service.blockRider(
                rider.getId(),
                "fraud confirmed",
                Optional.of(new CurrentActor("admin", user.getId(), "admin", Set.of("OPS_ADMIN"))),
                "req-1",
                "trace-1",
                "127.0.0.1",
                "JUnit"
        );

        assertThat(view.status()).isEqualTo("BLOCKED");
        assertThat(view.fraudHold()).isTrue();
        verify(adminAuditService).log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldBlockDriverAndAuditAction() {
        DriverProfileEntity driver = new DriverProfileEntity();
        driver.setId(UUID.fromString("40000000-0000-0000-0000-000000000001"));
        driver.setDriverStatus(DriverStatus.ACTIVE);
        driver.setRiskScore(new BigDecimal("12.50"));
        driver.setAverageRating(new BigDecimal("4.90"));
        driver.setDriverCode("DRIVER-1");
        var user = new com.ridingplatform.identity.infrastructure.persistence.UserProfileEntity();
        user.setId(UUID.fromString("41000000-0000-0000-0000-000000000001"));
        user.setDisplayName("Driver One");
        user.setEmail("driver@example.com");
        driver.setUserProfile(user);
        when(driverProfileJpaRepository.findById(driver.getId())).thenReturn(Optional.of(driver));
        when(driverProfileJpaRepository.save(any(DriverProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminProfileView view = service.blockDriver(
                driver.getId(),
                "ops block",
                Optional.of(new CurrentActor("admin", user.getId(), "admin", Set.of("OPS_ADMIN"))),
                "req-1",
                "trace-1",
                "127.0.0.1",
                "JUnit"
        );

        assertThat(view.status()).isEqualTo("BLOCKED");
        assertThat(view.fraudBlocked()).isTrue();
        verify(adminAuditService).log(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
