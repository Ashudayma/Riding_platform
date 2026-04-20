package com.ridingplatform.ride.application;

import com.ridingplatform.driver.application.DriverDispatchCandidate;
import java.math.BigDecimal;
import java.util.UUID;

public record DriverAssignmentCandidateScore(
        UUID driverProfileId,
        UUID vehicleId,
        double totalScore,
        int etaSeconds,
        double distanceMeters,
        BigDecimal rating,
        BigDecimal acceptanceRate,
        BigDecimal cancellationRate,
        long idleTimeSeconds,
        BigDecimal riskScore,
        double distanceComponent,
        double etaComponent,
        double ratingComponent,
        double acceptanceComponent,
        double cancellationComponent,
        double idleTimeComponent,
        double fraudPenaltyComponent,
        double vehicleCompatibilityComponent,
        double availabilityComponent,
        DriverDispatchCandidate candidate
) {
}
