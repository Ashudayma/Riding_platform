package com.ridingplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform")
public record ApplicationProperties(
        String name,
        Dispatch dispatch,
        SharedRide sharedRide,
        Realtime realtime,
        Fraud fraud
) {

    public record Dispatch(
            int assignmentTimeoutSeconds,
            int candidateLimit,
            int candidateCacheSeconds,
            int timeoutScanBatchSize,
            int retryBackoffSeconds,
            int maxDispatchRounds,
            double averageCitySpeedKph,
            Score score
    ) {
    }

    public record Score(
            double distanceWeight,
            double etaWeight,
            double ratingWeight,
            double acceptanceWeight,
            double cancellationWeight,
            double idleTimeWeight,
            double fraudPenaltyWeight,
            double vehicleCompatibilityWeight,
            double availabilityWeight,
            double maxIdleTimeSeconds,
            double maxEtaSeconds,
            double maxDistanceMeters
    ) {
    }

    public record SharedRide(
            int maxDetourMinutes,
            int maxPickupStops,
            int maxDropStops,
            int maxPickupWaitMinutes,
            int maxDetourPercentage,
            int maxPoolSearchRadiusMeters,
            double baseCompatibilityThreshold,
            double pricingDiscountFactor
    ) {
    }

    public record Realtime(
            long brokerHeartbeatMillis,
            long outboundLocationThrottleMillis,
            long minimumLocationDistanceMeters,
            long staleSessionSeconds
    ) {
    }

    public record Fraud(
            int repeatedCancellationThreshold,
            int failedPaymentThreshold,
            int promoAbuseThreshold,
            int shortTripRepeatThreshold,
            int cancellationWindowHours,
            int paymentWindowHours,
            int promoWindowHours,
            int shortTripWindowHours,
            int accountAnomalyWindowHours,
            int gpsHighAccuracyMeters,
            int impossibleSpeedKph,
            int routeDeviationThresholdPercent,
            int maxShortTripDistanceMeters,
            int maxShortTripDurationSeconds,
            int mediumRiskThreshold,
            int highRiskThreshold,
            int criticalRiskThreshold,
            int flagDedupeHours
    ) {
    }
}
