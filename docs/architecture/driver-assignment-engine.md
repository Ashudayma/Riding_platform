# Driver Assignment Engine

## Overview

The driver assignment engine is a dispatch component that listens to `riding-platform.ride.requested`,
finds nearby eligible drivers, computes a weighted score for each candidate, and atomically reserves the
best available driver for the ride request.

The implementation is intentionally split between:

- Redis-backed/live geospatial candidate discovery via `DriverAvailabilityService`
- PostgreSQL row-locking as the source of truth for assignment claims
- Kafka event publication for downstream notifications, analytics, and operational monitoring
- a persistent `ride.driver_assignment_attempt` table for retries, fallback, and auditability

## Scoring Model

The weighted score is configurable under `platform.dispatch.score`.

Current inputs:

- distance from pickup
- ETA derived from configurable average city speed
- driver rating
- acceptance rate
- cancellation rate
- current idle time using `driver.driver_availability.available_since`
- fraud/risk penalty using `driver.driver_profile.risk_score`
- vehicle compatibility
- online and availability state

The engine filters out clearly ineligible drivers first:

- not online
- not `AVAILABLE`
- already carrying another `current_ride_id`
- blocked/risk-blocked drivers
- incompatible vehicle type
- insufficient seat capacity

Only eligible candidates are scored and ranked.

## Concurrency Strategy

Correctness is enforced with database row locks:

- `ride_request` is locked with `PESSIMISTIC_WRITE`
- the shadow `ride` row is locked with `PESSIMISTIC_WRITE`
- each candidate `driver_availability` row is locked before claim

This prevents double assignment even if multiple dispatch workers or retries race on the same ride or driver.

Claim flow:

1. Lock ride request and ride.
2. Load and rank candidates.
3. Lock the candidate driver availability row.
4. Re-check eligibility under lock.
5. Reserve the driver by setting `current_ride_id`.
6. Move ride and ride request to `DRIVER_ASSIGNED`.
7. Persist a `driver_assignment_attempt` row with timeout metadata.

## Timeout And Reassignment

Each assignment attempt is stored with:

- `dispatch_round`
- `attempt_no`
- `assignment_status`
- `weighted_score`
- `expires_at`
- score breakdown JSON

A scheduled timeout worker scans expired `PENDING_DRIVER_RESPONSE` attempts, marks them `TIMED_OUT`,
releases the driver reservation, moves the ride back to `SEARCHING_DRIVER`, and triggers redispatch.

## Kafka Events

Published topics:

- `riding-platform.ride.requested`
- `riding-platform.dispatch.driver-assigned`
- `riding-platform.dispatch.assignment-failed`
- `riding-platform.dispatch.assignment-timed-out`

## Shared Ride Extension Path

To extend this engine for shared rides later:

- replace single-request candidate lookup with group-aware matching input from `shared_ride_candidate`
- score a driver against both driver quality and pooling route-fit
- add seat-reservation logic at the shared-group level before driver claim
- consider detour cost, incremental pickup delay, and group compatibility as additional weighted factors
- reserve both driver capacity and shared-group capacity in the same transaction boundary

The existing `driver_assignment_attempt` table and dispatch round logic can remain in place; the main change
is that the scored unit becomes `driver + pooling plan` rather than `driver` alone.
