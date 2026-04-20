# Production Hardening And Scaling

This document focuses on the current modular-monolith architecture and highlights the next scale risks, concrete tuning priorities, and test strategy.

## 1. Bottleneck Analysis For Current Architecture

### Likely first bottlenecks

#### Booking APIs

- Synchronous booking currently touches validation, pricing, persistence, cache invalidation, idempotency, and Kafka publication.
- Risk:
  booking latency grows when PostgreSQL commit time or downstream sync work grows.

#### Driver location updates

- High-frequency writes hit:
  - authenticated HTTP ingress
  - PostgreSQL availability update path
  - Redis driver state update path
  - Kafka event publication
- Risk:
  the database becomes an unnecessary write bottleneck if every location point is persisted synchronously.

#### Dispatch engine

- Dispatch consumes ride request events, searches nearby drivers, scores candidates, and locks state for assignment.
- Risk:
  candidate search and assignment locking become the first CPU and coordination hotspot under surge.

#### WebSocket layer

- The current broker is Spring's in-memory simple broker.
- Risk:
  it is good for first production foundations, but it becomes a ceiling for large fanout and multi-instance state coordination.

#### Kafka consumers

- Current listeners are present, but consumer concurrency and partition-aware tuning are still minimal.
- Risk:
  fraud, notifications, realtime, and dispatch consumers can lag during traffic spikes.

#### Fraud pipeline

- Rule-based evaluation is event driven and uses Redis windows plus DB persistence.
- Risk:
  expensive synchronous enrichment or duplicate event processing can cause lag.

## 2. Scaling Strategy By Area

### Booking APIs

#### Strategy

- Keep booking request path thin and deterministic.
- Move all non-user-blocking side effects behind Kafka and outbox.
- Cache pricing rules and surge factors aggressively in Redis.
- Use strict idempotency on booking and payment-like endpoints.

#### Production upgrades

- Introduce an outbox table for ride requested, quote generated, and payment events.
- Separate command APIs from heavy read models.
- Use connection pool tuning and query budgets for booking requests.
- Add admission control when DB latency spikes.

#### Suggested target

- p95 booking latency under 500 to 800 ms at normal peak
- p99 under 1.5 s

### Driver Location Updates

#### Strategy

- Make Redis the hot write path for current driver state.
- Persist full history asynchronously in batches.
- Throttle fanout by distance and time.

#### Current strengths

- `DriverRedisStateService` already keeps current geo and state in Redis.
- `DriverLocationFanoutGuard` already prevents excessive realtime fanout.

#### Production upgrades

- Persist history from Kafka rather than synchronously in request path.
- Reduce DB write frequency to heartbeat snapshots, not every point.
- Introduce sampling by:
  - minimum distance delta
  - minimum time delta
  - ride phase
- Use Redis GEO for city-local fast lookup, PostGIS for fallback or auditing queries.

### Dispatch Engine

#### Strategy

- Separate candidate generation from candidate scoring and assignment state mutation.
- Make candidate shortlist retrieval Redis-first, PostGIS-second.
- Keep one ride request key ordered to one consumer partition or lock domain.

#### Production upgrades

- Partition ride-request dispatch events by `rideRequestId`.
- Use Kafka partitions and consumer concurrency sized to city or region throughput.
- Add dispatch round counters and time budget protection.
- Cache candidate score inputs for the lifetime of one dispatch attempt.
- Add fast fail when no nearby drivers exist instead of re-querying immediately.

### WebSocket Layer

#### Strategy

- Keep HTTP APIs stateless and horizontally scaled.
- Move session coordination away from local memory when connection counts grow.

#### Current risk

- `WebSocketSessionRegistry` is in-memory, so presence and stale-session state are node local.
- Spring simple broker does not scale like a dedicated message broker.

#### Production upgrades

- Short term:
  sticky sessions at the load balancer.
- Medium term:
  externalize websocket broker using:
  - RabbitMQ STOMP broker relay, or
  - dedicated websocket gateway layer.
- Store presence/session state in Redis.
- Throttle location fanout per ride and per subscriber class.

### Kafka Consumers

#### Strategy

- Align partitions, concurrency, and consumer groups by workload.
- Keep consumers idempotent and fast.

#### Production upgrades

- Add listener container concurrency tuned by topic:
  - dispatch: low to medium, strict ordering
  - notifications: higher concurrency
  - fraud: medium to high with bounded enrichment cost
  - location history: high throughput batch consumers
- Add dead-letter topics for poison messages.
- Track consumer lag in Prometheus and Grafana.

### Fraud Detection Pipeline

#### Strategy

- Keep rule counters in Redis windows.
- Run rule evaluation asynchronously off Kafka.
- Persist only meaningful fraud cases and profile changes.

#### Production upgrades

- Split counters, rule evaluation, and case management into distinct consumer stages if lag appears.
- Add event dedupe keys by event id.
- Isolate ML scoring behind timeout and fallback protection.
- Add fraud-risk profile cache in Redis for dispatch and eligibility checks.

## 3. Redis Usage Tuning

### Current Redis roles

- driver geo and state
- dispatch candidate cache
- rate limiting
- fraud windows
- pricing cache
- ride history cache

### Tuning suggestions

- Use separate key prefixes by domain and TTL class.
- Add explicit TTLs everywhere for ephemeral keys.
- Use pipelining for multi-key state updates in hot paths.
- Prefer hashes for current driver state and strings for small immutable snapshots.
- Add Redis memory eviction policy appropriate for cache-only nodes:
  - `allkeys-lru` for cache node
  - avoid mixing durable and cache semantics in one Redis if possible
- For heavy scale:
  split Redis roles:
  - hot geo/session cache
  - rate limit/idempotency
  - pricing/config cache

### Suggested key families

- `drivers:geo:<city>`
- `drivers:state:<driverId>`
- `dispatch:candidates:<rideRequestId>`
- `fraud:window:<signal>:<subjectId>`
- `pricing:rules:<city>:<zone>:<rideType>`
- `presence:ws:<subject>`

## 4. PostgreSQL Indexing And Partitioning

### Indexing priorities

#### Ride domain

- `ride_request (request_status, requested_at desc)`
- `ride_request (rider_profile_id, requested_at desc)`
- `ride (driver_profile_id, lifecycle_status, assigned_at desc)`
- `ride_status_history (ride_request_id, changed_at desc)`

#### Driver domain

- `driver_availability (online_status, availability_status)`
- GiST index on `driver_availability.last_location`
- `driver_profile (driver_status, fraud_blocked)`

#### Fraud domain

- `fraud_flag (flag_status, created_at desc)`
- `fraud_flag (subject_type, subject_id, created_at desc)`
- `fraud_risk_profile (subject_type, subject_id)`

#### Admin queries

- Composite indexes matching dashboard filters:
  - rider status + fraud hold
  - driver status + fraud blocked
  - pricing rule city + active + effective from

### Partitioning suggestions

Partition append-heavy tables by time:

- `tracking.driver_location_history`
  monthly or weekly partitions
- `ride.ride_status_history`
  monthly partitions
- `admin.admin_audit_log`
  monthly partitions
- `fraud.fraud_signal_event`
  monthly partitions if event volume becomes large

### Partitioning rule of thumb

- do not partition every table
- partition only tables with high insert volume and time-based retention patterns

## 5. Geospatial Scaling Strategy

### Recommended tiered model

#### Tier 1: Redis GEO

- primary nearby-driver search for live dispatch
- city-local, low-latency, approximate filtering

#### Tier 2: PostGIS

- fallback queries
- analytics
- compliance and audit use cases
- backfill or consistency verification

### Scaling approach

- shard driver geo keys by city or region:
  - `drivers:geo:delhi`
  - `drivers:geo:blr`
- avoid one global GEO key at very high scale
- route driver search to region-local caches first
- compute geohash or S2 cell buckets for cross-city scale planning

## 6. Horizontal Scaling Approach

### Backend

- run multiple stateless backend instances behind a load balancer
- keep request handlers stateless
- externalize session and cache state
- scale based on:
  - CPU
  - request rate
  - p95 latency

### Dispatch

- scale consumers by Kafka partitions, not just pod count
- keep one partition ordering guarantee per ride request

### WebSocket

- short term:
  sticky sessions
- longer term:
  dedicated realtime tier or broker relay

### AI/Fraud service

- independently autoscale inference workers
- enforce timeouts and circuit breaking from backend callers

## 7. Load Testing Strategy

Use k6 first because it is lightweight and versionable in-repo. JMeter is still useful for protocol-rich scenarios, but k6 is the better default here.

### Test layers

1. API smoke tests
2. peak booking tests
3. sustained location ingestion tests
4. dispatch surge tests
5. websocket concurrency tests
6. soak tests for 30 to 120 minutes

### Metrics to capture

- HTTP p50, p95, p99
- DB CPU, locks, active connections
- Redis latency and memory
- Kafka lag by consumer group
- websocket active sessions and send latency
- dispatch timeout percentage
- fraud processing lag

## 8. Sample Load Tests

See:

- [infra/loadtest/README.md](/d:/Riding_Platform/infra/loadtest/README.md)
- [infra/loadtest/k6/booking-flow.js](/d:/Riding_Platform/infra/loadtest/k6/booking-flow.js)
- [infra/loadtest/k6/driver-location-updates.js](/d:/Riding_Platform/infra/loadtest/k6/driver-location-updates.js)

## 9. SLO And SLA Suggestions

### Suggested SLOs

- Booking API availability:
  99.9%
- Booking API p95 latency:
  < 800 ms
- Driver location ingest p95 latency:
  < 300 ms
- Dispatch assignment decision time p95:
  < 2 s
- WebSocket ride update delivery p95:
  < 1 s
- Fraud alert processing lag p95:
  < 30 s

### Suggested customer-facing SLA framing

- Rider booking service monthly availability:
  99.9%
- Driver dispatch and trip execution services:
  99.9%
- Admin analytics and dashboards:
  99.5%

## 10. Resilience Improvements

### Immediate improvements

- Add outbox pattern for Kafka publication
- Add DLQs for Kafka consumers
- Add retry budgets rather than infinite retries
- Add circuit breakers around AI service calls
- Add bounded thread pools for websocket and consumer workloads

### Next resilience layer

- chaos test Redis, Kafka, and Keycloak dependency loss
- degrade gracefully:
  - disable shared ride matching under pressure
  - fall back to normal ride assignment
  - bypass non-critical fraud enrichment
- isolate heavy admin reads from hot rider/driver traffic

## 11. Recommended Next Implementation Steps

1. Add Kafka listener concurrency and DLQ configuration.
2. Move driver location persistence fully async if not already.
3. Split Redis keys by city and add geo sharding.
4. Partition large append-only PostgreSQL tables.
5. Replace in-memory websocket presence with Redis-backed presence.
6. Run baseline k6 tests and record first capacity envelope.
