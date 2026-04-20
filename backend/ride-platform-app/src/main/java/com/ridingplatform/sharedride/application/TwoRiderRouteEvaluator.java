package com.ridingplatform.sharedride.application;

import com.ridingplatform.ride.infrastructure.persistence.StopType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TwoRiderRouteEvaluator implements PoolRouteEvaluator {

    private static final double METERS_PER_DEGREE = 111_000d;

    @Override
    public List<PoolRoutePlan> evaluate(PoolRouteEvaluationContext context) {
        List<List<PoolRouteStop>> permutations = List.of(
                List.of(context.existingPickup(), context.newPickup(), context.existingDrop(), context.newDrop()),
                List.of(context.existingPickup(), context.newPickup(), context.newDrop(), context.existingDrop()),
                List.of(context.newPickup(), context.existingPickup(), context.existingDrop(), context.newDrop()),
                List.of(context.newPickup(), context.existingPickup(), context.newDrop(), context.existingDrop())
        );
        List<PoolRoutePlan> feasible = new ArrayList<>();
        for (List<PoolRouteStop> stops : permutations) {
            PoolRoutePlan plan = evaluatePermutation(stops);
            if (plan != null
                    && plan.peakSeatUsage() <= context.seatCapacity()
                    && plan.existingRiderPickupWaitSeconds() <= context.policy().maxPickupWaitMinutes() * 60
                    && plan.newRiderPickupWaitSeconds() <= context.policy().maxPickupWaitMinutes() * 60
                    && plan.existingRiderDetourSeconds() <= context.policy().maxDetourMinutes() * 60
                    && plan.newRiderDetourSeconds() <= context.policy().maxDetourMinutes() * 60
                    && plan.detourPercentageExisting() <= context.policy().maxDetourPercentage()
                    && plan.detourPercentageNew() <= context.policy().maxDetourPercentage()) {
                feasible.add(plan);
            }
        }
        return feasible.stream()
                .sorted(Comparator.comparingInt(PoolRoutePlan::totalDurationSeconds))
                .toList();
    }

    private PoolRoutePlan evaluatePermutation(List<PoolRouteStop> stops) {
        int totalDistance = 0;
        int totalDuration = 0;
        int peakSeats = 0;
        int seats = 0;

        Integer existingPickupAt = null;
        Integer existingDropAt = null;
        Integer newPickupAt = null;
        Integer newDropAt = null;

        for (int index = 0; index < stops.size(); index++) {
            PoolRouteStop stop = stops.get(index);
            if (index > 0) {
                int legDistance = estimateDistanceMeters(stops.get(index - 1), stop);
                totalDistance += legDistance;
                totalDuration += estimateDurationSeconds(legDistance);
            }
            if (stop.stopType() == StopType.PICKUP) {
                seats += stop.passengerCount();
            } else if (stop.stopType() == StopType.DROPOFF) {
                seats -= stop.passengerCount();
            }
            peakSeats = Math.max(peakSeats, seats);

            switch (stop.role()) {
                case PICKUP_EXISTING -> existingPickupAt = totalDuration;
                case DROPOFF_EXISTING -> existingDropAt = totalDuration;
                case PICKUP_NEW -> newPickupAt = totalDuration;
                case DROPOFF_NEW -> newDropAt = totalDuration;
            }
        }

        if (existingPickupAt == null || existingDropAt == null || newPickupAt == null || newDropAt == null) {
            return null;
        }

        int baseExisting = estimateDurationSeconds(estimateDistanceMeters(stopsByRole(stops, RouteStopRole.PICKUP_EXISTING), stopsByRole(stops, RouteStopRole.DROPOFF_EXISTING)));
        int baseNew = estimateDurationSeconds(estimateDistanceMeters(stopsByRole(stops, RouteStopRole.PICKUP_NEW), stopsByRole(stops, RouteStopRole.DROPOFF_NEW)));
        int existingTripDuration = existingDropAt - existingPickupAt;
        int newTripDuration = newDropAt - newPickupAt;
        int existingDetour = Math.max(0, existingTripDuration - baseExisting);
        int newDetour = Math.max(0, newTripDuration - baseNew);

        return new PoolRoutePlan(
                code(stops),
                stops,
                totalDistance,
                totalDuration,
                existingPickupAt,
                newPickupAt,
                existingDetour,
                newDetour,
                peakSeats,
                percentage(existingDetour, baseExisting),
                percentage(newDetour, baseNew)
        );
    }

    private PoolRouteStop stopsByRole(List<PoolRouteStop> stops, RouteStopRole role) {
        return stops.stream().filter(stop -> stop.role() == role).findFirst().orElseThrow();
    }

    private int estimateDistanceMeters(PoolRouteStop from, PoolRouteStop to) {
        double latDiff = to.latitude() - from.latitude();
        double lonDiff = to.longitude() - from.longitude();
        return Math.max(200, (int) Math.round(Math.sqrt(latDiff * latDiff + lonDiff * lonDiff) * METERS_PER_DEGREE));
    }

    private int estimateDurationSeconds(int distanceMeters) {
        return Math.max(60, (int) Math.round(distanceMeters / 7.0d));
    }

    private double percentage(int part, int base) {
        if (base <= 0) {
            return 0;
        }
        return (part * 100.0d) / base;
    }

    private String code(List<PoolRouteStop> stops) {
        return stops.stream()
                .map(stop -> switch (stop.role()) {
                    case PICKUP_EXISTING -> "PA";
                    case DROPOFF_EXISTING -> "DA";
                    case PICKUP_NEW -> "PB";
                    case DROPOFF_NEW -> "DB";
                })
                .reduce((left, right) -> left + "->" + right)
                .orElse("EMPTY");
    }
}
