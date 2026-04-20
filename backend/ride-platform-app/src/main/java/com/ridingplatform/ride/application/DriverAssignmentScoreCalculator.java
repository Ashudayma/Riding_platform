package com.ridingplatform.ride.application;

import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.driver.application.DriverDispatchCandidate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class DriverAssignmentScoreCalculator {

    private final ApplicationProperties applicationProperties;
    private final Clock clock;

    public DriverAssignmentScoreCalculator(ApplicationProperties applicationProperties, Clock clock) {
        this.applicationProperties = applicationProperties;
        this.clock = clock;
    }

    public DriverAssignmentCandidateScore score(DriverDispatchCandidate candidate, double distanceMeters) {
        ApplicationProperties.Score weights = applicationProperties.dispatch().score();
        int etaSeconds = estimateEtaSeconds(distanceMeters);
        long idleTimeSeconds = candidate.availableSince() == null
                ? 0
                : Math.max(0, Duration.between(candidate.availableSince(), Instant.now(clock)).getSeconds());

        double distanceComponent = weights.distanceWeight() * inverseNormalize(distanceMeters, weights.maxDistanceMeters());
        double etaComponent = weights.etaWeight() * inverseNormalize(etaSeconds, weights.maxEtaSeconds());
        double ratingComponent = weights.ratingWeight() * normalize(candidate.averageRating(), 5.0d);
        double acceptanceComponent = weights.acceptanceWeight() * normalize(candidate.acceptanceRate(), 100.0d);
        double cancellationComponent = weights.cancellationWeight() * inverseNormalize(candidate.cancellationRate().doubleValue(), 100.0d);
        double idleTimeComponent = weights.idleTimeWeight() * normalize(idleTimeSeconds, weights.maxIdleTimeSeconds());
        double fraudPenaltyComponent = weights.fraudPenaltyWeight() * normalize(candidate.riskScore(), 100.0d);
        double vehicleCompatibilityComponent = weights.vehicleCompatibilityWeight();
        double availabilityComponent = weights.availabilityWeight();
        double totalScore = distanceComponent
                + etaComponent
                + ratingComponent
                + acceptanceComponent
                + cancellationComponent
                + idleTimeComponent
                + vehicleCompatibilityComponent
                + availabilityComponent
                - fraudPenaltyComponent;

        return new DriverAssignmentCandidateScore(
                candidate.driverProfileId(),
                candidate.vehicleId(),
                round(totalScore),
                etaSeconds,
                round(distanceMeters),
                candidate.averageRating(),
                candidate.acceptanceRate(),
                candidate.cancellationRate(),
                idleTimeSeconds,
                candidate.riskScore(),
                round(distanceComponent),
                round(etaComponent),
                round(ratingComponent),
                round(acceptanceComponent),
                round(cancellationComponent),
                round(idleTimeComponent),
                round(fraudPenaltyComponent),
                round(vehicleCompatibilityComponent),
                round(availabilityComponent),
                candidate
        );
    }

    public int estimateEtaSeconds(double distanceMeters) {
        double averageSpeedMps = Math.max(1.0d, applicationProperties.dispatch().averageCitySpeedKph() / 3.6d);
        return (int) Math.max(60, Math.round(distanceMeters / averageSpeedMps));
    }

    private double inverseNormalize(double value, double max) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, 1.0d - Math.min(value, max) / max);
    }

    private double normalize(BigDecimal value, double max) {
        return normalize(value == null ? 0 : value.doubleValue(), max);
    }

    private double normalize(long value, double max) {
        return normalize((double) value, max);
    }

    private double normalize(double value, double max) {
        if (max <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(value, max) / max);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
