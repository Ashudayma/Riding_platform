package com.ridingplatform.ride.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.driver.application.DriverDispatchCandidate;
import com.ridingplatform.driver.infrastructure.persistence.AvailabilityStatus;
import com.ridingplatform.driver.infrastructure.persistence.OnlineStatus;
import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DriverAssignmentScoreCalculatorTest {

    @Test
    void shouldScoreCloserSaferDriverHigher() {
        ApplicationProperties properties = new ApplicationProperties(
                "riding-platform",
                new ApplicationProperties.Dispatch(
                        15,
                        10,
                        20,
                        100,
                        2,
                        3,
                        22,
                        new ApplicationProperties.Score(0.24, 0.22, 0.12, 0.12, 0.10, 0.08, 0.08, 0.02, 0.02, 1800, 1800, 5000)
                ),
                new ApplicationProperties.SharedRide(12, 3, 3, 10, 30, 4000, 0.55d, 0.18d),
                new ApplicationProperties.Realtime(10000, 1000, 20, 90),
                new ApplicationProperties.Fraud(4, 3, 5, 4, 24, 6, 24, 12, 24, 120, 140, 35, 1500, 420, 25, 50, 80, 12)
        );
        DriverAssignmentScoreCalculator calculator = new DriverAssignmentScoreCalculator(
                properties,
                Clock.fixed(Instant.parse("2026-04-18T12:00:00Z"), ZoneOffset.UTC)
        );

        DriverDispatchCandidate strongCandidate = new DriverDispatchCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                VehicleType.SEDAN,
                (short) 4,
                AvailabilityStatus.AVAILABLE,
                OnlineStatus.ONLINE,
                null,
                new BigDecimal("4.95"),
                new BigDecimal("96.00"),
                new BigDecimal("2.00"),
                new BigDecimal("5.00"),
                false,
                Instant.parse("2026-04-18T11:45:00Z"),
                Instant.parse("2026-04-18T11:59:50Z")
        );
        DriverDispatchCandidate weakCandidate = new DriverDispatchCandidate(
                UUID.randomUUID(),
                UUID.randomUUID(),
                VehicleType.SEDAN,
                (short) 4,
                AvailabilityStatus.AVAILABLE,
                OnlineStatus.ONLINE,
                null,
                new BigDecimal("4.60"),
                new BigDecimal("80.00"),
                new BigDecimal("12.00"),
                new BigDecimal("35.00"),
                false,
                Instant.parse("2026-04-18T11:58:00Z"),
                Instant.parse("2026-04-18T11:59:40Z")
        );

        DriverAssignmentCandidateScore strongScore = calculator.score(strongCandidate, 450);
        DriverAssignmentCandidateScore weakScore = calculator.score(weakCandidate, 1800);

        assertThat(strongScore.totalScore()).isGreaterThan(weakScore.totalScore());
        assertThat(strongScore.etaSeconds()).isLessThan(weakScore.etaSeconds());
    }
}
