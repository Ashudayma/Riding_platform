package com.ridingplatform.ride.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.pricing.application.FareQuoteResult;
import com.ridingplatform.pricing.application.PricingEngineService;
import com.ridingplatform.ride.domain.RideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RidePricingServiceTest {

    private final PricingEngineService pricingEngineService = Mockito.mock(PricingEngineService.class);

    private RidePricingService ridePricingService;

    @BeforeEach
    void setUp() {
        ridePricingService = new RidePricingService(pricingEngineService);
    }

    @Test
    void shouldDelegateEstimateToPricingEngineAndMapResult() {
        when(pricingEngineService.estimate(any())).thenReturn(new FareQuoteResult(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                RideType.SHARED,
                "DELHI_NCR",
                "CENTRAL",
                1,
                new BigDecimal("60.00"),
                new BigDecimal("90.00"),
                new BigDecimal("20.00"),
                new BigDecimal("1.2000"),
                new BigDecimal("18.00"),
                new BigDecimal("12.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("9.50"),
                BigDecimal.ZERO,
                new BigDecimal("25.00"),
                new BigDecimal("184.50"),
                "INR",
                12000,
                1800,
                Instant.parse("2026-04-18T12:10:00Z"),
                null
        ));

        FareEstimateResult result = ridePricingService.estimate(new FareEstimateCommand(
                UUID.fromString("30000000-0000-0000-0000-000000000001"),
                RideType.SHARED,
                (short) 1,
                VehicleType.SEDAN,
                28.6139,
                77.2090,
                "Connaught Place",
                28.4595,
                77.0266,
                "Cyber Hub",
                List.of(new RideStopCommand(StopTypeCommand.WAYPOINT, 28.5700, 77.1800, "Waypoint", "Delhi"))
        ));

        assertThat(result.rideType()).isEqualTo(RideType.SHARED);
        assertThat(result.poolingDiscountAmount()).isEqualTo(new BigDecimal("25.00"));
        assertThat(result.currencyCode()).isEqualTo("INR");
    }
}
