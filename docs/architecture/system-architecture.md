# Production-Grade Ride Platform Architecture

## 1. High-Level Architecture

The platform should begin as a **modular monolith with event-driven seams** and evolve into selective microservices only when operational need is proven.

### Core architectural principles

- Domain-driven modular boundaries inside one deployable backend
- Clean architecture within each domain module
- Synchronous APIs for user-facing command/query flows
- Kafka for domain-event propagation and future service extraction
- Redis for low-latency state, caching, and geospatial/ephemeral use cases
- PostgreSQL as source of truth for transactional state
- Keycloak for identity, roles, and token issuance
- WebSocket for live ride state and location updates
- Observability by default: metrics, structured logs, tracing readiness

### Logical component view

1. `API Gateway / BFF layer`
   Handles web/mobile-facing concerns later if split out. Initially served by the backend app with REST and WebSocket endpoints.
2. `Core Platform Backend`
   Contains rider, driver, ride, dispatch, shared ride, pricing, fraud, admin, and notification orchestration modules.
3. `Data Layer`
   PostgreSQL for consistency, Redis for fast-changing state, Kafka for durable asynchronous events.
4. `Identity`
   Keycloak for OIDC, RBAC, token management, and optional social login/federation.
5. `AI / Risk Services`
   Separate Python or JVM service for fraud/risk scoring, route optimization experiments, and advanced pooling heuristics.

## 2. Modular Monolith vs Microservices

### Recommendation

Start with a **modular monolith**.

### Why this is the right production choice

- Dispatch, shared ride matching, pricing, and fraud decisions interact constantly and benefit from low-latency in-process coordination early on.
- Domain rules will change rapidly while product-market fit is refined.
- A microservice-first design would add operational drag: distributed transactions, cross-service debugging, deployment coordination, contract drift, and higher infra cost.
- Production quality does not require immediate microservices. It requires strong boundaries, observability, resilience patterns, and clear extraction paths.

### When to extract later

Extract a module into a service when several of these are true:

- Independent scaling profile
- Separate team ownership
- Frequent release cadence mismatch
- Resource isolation need
- Domain complexity large enough to justify operational overhead

## 3. Future Service Boundaries

These are the recommended extraction candidates and their future ownership lines:

### Early extraction candidates

- `dispatch-service`
  Driver search, ranking, assignment, retry, offer timeout
- `tracking-service`
  Driver/rider location ingestion, geospatial queries, live trip streams
- `pricing-service`
  Fare estimation, surge logic, discounts, pooling savings
- `fraud-risk-service`
  Rule engine, ML scoring, anomaly detection, case generation

### Medium-term extraction candidates

- `ride-service`
  Trip lifecycle and orchestration if transaction load gets large
- `notification-service`
  Push/SMS/email/in-app communication
- `identity-bff`
  If auth/session concerns grow beyond basic resource-server use

### Usually keep centralized longer

- `admin-service`
- `reporting-service`
- `catalog/config-service`

## 4. Backend Module and Package Structure

Recommended package-by-domain and layer-by-responsibility structure:

```text
com.ridingplatform
|-- bootstrap
|-- common
|   |-- architecture
|   |-- domain
|   |-- exception
|   |-- id
|   |-- messaging
|   |-- security
|   `-- web
|-- identity
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
|-- rider
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
|-- driver
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
|-- ride
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
|-- sharedride
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
|-- dispatch
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
|-- pricing
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
|-- tracking
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
|-- fraud
|   |-- application
|   |-- domain
|   |-- infrastructure
|   `-- interfaces
`-- admin
    |-- application
    |-- domain
    |-- infrastructure
    `-- interfaces
```

### Layer responsibilities

- `domain`
  Entities, aggregates, value objects, business rules, repository interfaces, domain events
- `application`
  Use cases, command/query handlers, orchestration, transaction boundaries
- `infrastructure`
  JPA repositories, Kafka publishers, Redis adapters, Keycloak adapters, external integration
- `interfaces`
  REST controllers, WebSocket handlers, DTOs, request/response mapping

## 5. Database Design Overview

Use PostgreSQL as the transactional system of record.

### Core schemas

- `identity`
  User profile shadow tables, role mappings, audit references
- `rider`
  Rider profile, rider preferences, trusted contacts, payment references
- `driver`
  Driver profile, vehicle, onboarding status, compliance docs metadata, driver stats
- `ride`
  Ride requests, trips, stops, assignments, trip state transitions
- `dispatch`
  Assignment attempts, candidate ranking snapshots, dispatch decisions
- `pricing`
  Fare quotes, pricing rules, surge windows, discount applications
- `fraud`
  risk_assessment, fraud_signal, fraud_case, manual_review_action
- `admin`
  audit_event, platform_config, feature_flag
- `outbox`
  Transactional outbox for Kafka event publication

### Important table concepts

- `ride_request`
  Origin, destination, ride type, requested_at, status, pricing quote ref
- `trip`
  Accepted ride instance, rider/driver refs, status timeline, final fare
- `trip_stop`
  Ordered pickup/drop checkpoints for standard and shared rides
- `shared_pool_candidate`
  Pooling compatibility decisions and match score
- `driver_availability`
  High-level persisted availability snapshot
- `driver_assignment_attempt`
  Candidate driver, ranking factors, accept/reject/timeout reason
- `fare_quote`
  Base fare, time distance fare, surge multiplier, pooling discount
- `risk_assessment`
  Subject type, model score, rules triggered, action recommendation

### Database design patterns

- UUID primary keys
- Partition large append-heavy tables like trip events and location history
- Flyway for migrations
- Strong indexes on status, geospatial buckets, timestamps, rider/driver IDs
- Avoid persisting every real-time location point in the primary write path unless needed; aggregate hot data in Redis or a dedicated time-series store

## 6. Kafka Topic Design Overview

Kafka should be present from the beginning, even if some processing is initially in-process.

### Topic naming convention

`riding-platform.<domain>.<event>`

### Core topics

- `riding-platform.ride.requested`
- `riding-platform.ride.accepted`
- `riding-platform.ride.cancelled`
- `riding-platform.ride.completed`
- `riding-platform.dispatch.assignment-requested`
- `riding-platform.dispatch.driver-offered`
- `riding-platform.dispatch.assignment-confirmed`
- `riding-platform.pricing.quote-generated`
- `riding-platform.pricing.surge-updated`
- `riding-platform.sharedride.match-evaluated`
- `riding-platform.sharedride.pool-formed`
- `riding-platform.tracking.location-updated`
- `riding-platform.fraud.signal-detected`
- `riding-platform.fraud.assessment-completed`
- `riding-platform.admin.audit-recorded`

### Kafka design guidelines

- Use Avro or Protobuf later for schema governance
- Include event ID, aggregate ID, event type, occurred-at, trace ID, version
- Use outbox pattern to avoid dual-write issues
- Key by rider ID, driver ID, or trip ID depending on ordering requirement
- Retain tracking topics differently from business-event topics

## 7. Redis Usage Plan

Redis should be used for ephemeral, hot, and low-latency state.

### Recommended use cases

- Nearby driver lookup cache
- Driver current location cache
- Rider session and temporary booking context
- Dispatch candidate shortlist cache
- Idempotency keys for booking/payment/fraud workflows
- Rate limiting and abuse throttling
- Short-lived fare estimate cache
- WebSocket subscription/session presence metadata
- Feature flag or config cache

### Data structure examples

- `GEO` for driver proximity search
- `HASH` for driver/rider online session state
- `SET` / `ZSET` for dispatch ranking queues
- `STRING` for idempotency and short-lived quotes
- `STREAM` only if you have a very specific lightweight local streaming use case; Kafka remains the durable event bus

## 8. Security Architecture with Keycloak

### Auth model

- Keycloak is the identity provider using OIDC/OAuth2
- Backend APIs act as JWT resource servers
- Frontend apps obtain tokens through Authorization Code + PKCE
- Service-to-service access later can use client credentials

### Keycloak realms and clients

- Realm: `riding-platform`
- Clients:
  - `riding-platform-web`
  - `riding-platform-rider-mobile`
  - `riding-platform-driver-mobile`
  - `riding-platform-api`
  - `riding-platform-admin`

### Recommended roles

- `rider`
- `driver`
- `platform-admin`
- `ops-admin`
- `fraud-analyst`
- `support-agent`
- `pricing-analyst`

### Security enforcement

- REST endpoints secured by role and scope
- Domain-level authorization checks inside application services
- Driver onboarding and compliance state checked before assignment eligibility
- Fraud flags can trigger step-up verification, booking holds, or admin review
- Audit all privileged admin operations

## 9. Real-Time and Shared Ride Design Notes

### Real-time updates

- Driver devices send location updates through authenticated endpoints
- Latest location lands in Redis first
- Trip-facing updates are pushed via WebSocket/STOMP or a lightweight topic-based socket protocol
- Important trip milestones also persist to PostgreSQL and publish to Kafka

### Shared ride optimization

- Start with rule-based heuristics:
  - route overlap threshold
  - max detour threshold
  - max seat capacity
  - ETA deviation limits
  - rider compatibility constraints
- Evolve later toward optimization service:
  - graph search
  - insertion heuristics
  - batch assignment
  - ML-assisted acceptance prediction

## 10. Fraud and Risk Detection Strategy

Use a hybrid model from day one:

- deterministic rules
- velocity checks
- device/account linkage
- geo anomalies
- payment anomaly markers
- driver/rider collusion patterns
- optional ML score from separate AI service

### Example suspicious signals

- Too many cancellations after assignment
- Repeated ride loops between the same accounts
- GPS spoofing indicators
- Driver location inconsistent with trip progression
- Multiple accounts sharing device fingerprints or payment instruments

## 11. Step-by-Step Implementation Roadmap

### Phase 1: Platform foundation

1. Stand up monorepo, infra, CI, coding standards.
2. Implement modular monolith skeleton.
3. Configure Keycloak JWT resource server.
4. Add Flyway, health checks, metrics, structured logging.

### Phase 2: Core ride flow

1. Build rider and driver registration/profile modules.
2. Implement ride request for normal rides.
3. Implement driver availability and driver candidate search.
4. Build assignment scoring using distance, ETA, rating, availability, and generated driver score.
5. Add ride accept/reject/timeout handling.

### Phase 3: Real-time trip operations

1. Add location ingestion APIs.
2. Store current location in Redis GEO.
3. Publish trip updates through WebSocket.
4. Persist major trip events and stream to Kafka.

### Phase 4: Shared ride and pooling

1. Add multi-stop trip model.
2. Implement shared-ride request flow.
3. Add pool compatibility heuristics and detour constraints.
4. Apply pooling-aware pricing reductions.

### Phase 5: Fraud and risk

1. Implement rule-based signals and velocity checks.
2. Add risk assessment workflow and alert queue.
3. Introduce AI risk scoring service.
4. Add admin review and fraud case management.

### Phase 6: Production hardening

1. Add outbox pattern and resilient consumers.
2. Add rate limiting and idempotency.
3. Add tracing, dashboards, alerts, and SLOs.
4. Introduce blue/green or rolling deployment strategy.
5. Load test dispatch and tracking flows.

## 12. Monorepo Structure

```text
.
|-- backend/
|   |-- pom.xml
|   |-- ride-platform-app/
|   |   |-- pom.xml
|   |   `-- src/
|   |-- libs/
|   |   |-- shared-kernel/
|   |   `-- testing/
|   `-- build/
|-- frontend/
|   |-- apps/
|   |   |-- rider-web/
|   |   |-- driver-ops/
|   |   `-- admin-dashboard/
|   `-- packages/
|       `-- ui-kit/
|-- infra/
|   |-- docker/
|   |-- kubernetes/
|   |-- terraform/
|   `-- observability/
|-- docs/
|   |-- architecture/
|   |-- adr/
|   |-- api/
|   `-- runbooks/
`-- ai-services/
    |-- risk-scoring/
    `-- pooling-optimizer/
```

This repository includes the first-pass foundation for the highlighted areas, while leaving room for those future directories to be expanded.
