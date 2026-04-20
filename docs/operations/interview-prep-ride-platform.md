# Ride Platform Interview Prep

## Project Summary For Resume

Built a production-grade ride-hailing and ride-sharing platform inspired by Uber using Java 21, Spring Boot, PostgreSQL, Redis, Kafka, Keycloak, WebSocket, Next.js, and a Python FastAPI AI service. Designed the platform as a modular monolith with event-driven seams to support normal rides, pooled rides, real-time tracking, fraud and risk detection, dynamic pricing, driver dispatch, admin operations, and production-style observability. Focused on clean domain boundaries, low-latency geospatial search, resilient asynchronous workflows, and a clear scaling path toward selective service extraction.

## Resume Bullet Points

- Architected and built a production-style ride-hailing platform in Java Spring Boot with PostgreSQL, Redis, Kafka, Keycloak, and WebSocket, supporting booking, dispatch, real-time trip tracking, pricing, notifications, admin operations, and fraud controls.
- Designed a shared ride matching engine with route permutation evaluation, detour constraints, seat-capacity checks, compatibility scoring, and pooled pricing hooks, enabling multi-pickup and multi-drop ride combinations on the same trip graph.
- Implemented low-latency driver state and dispatch foundations using Redis GEO for nearby-driver lookup, Kafka-driven assignment workflows, Prometheus and Grafana observability, structured logging, and a FastAPI AI service for fraud scoring and probability-based product intelligence.

## Complete System Design Explanation

### 1. High-level architecture

The platform is built as a modular monolith with event-driven seams.

Why that matters:

- ride booking, pricing, dispatch, fraud checks, and trip lifecycle are strongly coupled business flows
- early product evolution is faster with in-process coordination
- Kafka is still used so the architecture can evolve toward microservices later without rewriting the domain model

### 2. Main components

- Rider, driver, and admin frontends in Next.js
- Spring Boot backend exposing REST APIs and STOMP/WebSocket channels
- PostgreSQL as the source of truth for transactional state
- Redis for current driver state, nearby-driver geo search, rate limiting, idempotency, and short-lived caches
- Kafka for ride, dispatch, tracking, fraud, and notification events
- Keycloak for OIDC authentication and RBAC
- FastAPI AI service for fraud scoring, cancellation probability, driver acceptance probability, demand hotspots, and pooled compatibility scoring
- Prometheus, Grafana, Loki, and Promtail for observability

### 3. Core request flow

#### Booking

1. Rider calls fare estimate API.
2. Backend computes quote using pricing config, surge logic, and pooled discount rules.
3. Rider books a ride with an idempotency key.
4. Ride request is stored in PostgreSQL.
5. Ride requested event is published to Kafka.
6. Dispatch engine consumes the event and searches nearby eligible drivers.
7. Best driver is selected through weighted scoring.
8. Assignment is published through Kafka and pushed to rider and driver via WebSocket.

#### Driver location flow

1. Driver app sends authenticated location updates.
2. Current driver position is written to Redis GEO and driver state hash.
3. Location update event is published to Kafka.
4. Realtime layer fans location updates to subscribed riders and drivers.
5. Historical points can be persisted asynchronously for audit and analytics.

#### Fraud flow

1. Cancellation, payment, and tracking events land on Kafka.
2. Fraud pipeline evaluates Redis velocity windows and rule-based conditions.
3. Risk score is updated in PostgreSQL.
4. Driver or rider can be flagged, blocked, or surfaced to analysts.
5. Later, the Python AI service can enrich that decision with model inference.

## Major Trade-offs

### Modular monolith vs microservices

I chose a modular monolith first.

Benefits:

- simpler transactional consistency
- lower operational overhead
- faster feature iteration
- easier debugging across tightly coupled flows

Trade-off:

- independent scaling is less granular than true microservices

How I mitigated it:

- strict package and domain boundaries
- Kafka topics per domain event
- Redis and PostgreSQL access isolated behind domain services
- future extraction boundaries already identified: dispatch, tracking, pricing, fraud

### PostgreSQL + Redis + Kafka

Benefits:

- PostgreSQL gives strong consistency
- Redis handles hot, low-latency mutable state
- Kafka decouples heavy asynchronous work

Trade-off:

- more moving pieces than a simple CRUD system

Why it was still worth it:

- ride-hailing is not just CRUD
- low-latency search, dispatch retries, realtime fanout, and fraud pipelines need specialized infrastructure

### WebSocket in backend vs separate realtime gateway

Current choice:

- Spring WebSocket/STOMP in the main backend

Benefit:

- faster initial delivery
- simpler auth and domain integration

Trade-off:

- in-memory broker and node-local session registry become a scaling limit later

Planned evolution:

- sticky sessions first
- then external broker relay or dedicated realtime gateway

## Scaling Story

### Booking APIs

- idempotency support protects retries and duplicate submits
- pricing rules and surge factors are cacheable in Redis
- write path stays small: validate, persist, publish
- non-blocking downstream work is handled asynchronously through Kafka

Interview version:

"I kept the booking path synchronous only for user-critical work and pushed side effects like notifications, fraud evaluation, and some downstream reactions behind Kafka to protect booking latency."

### Driver location updates

- Redis GEO is used for current hot state
- current driver state is stored in Redis hashes for fast lookup
- location fanout is throttled by time and distance to avoid flooding subscribers
- location history can be persisted asynchronously instead of making PostgreSQL the hot path for every update

Interview version:

"I treated location updates as a high-frequency stream problem, not a normal CRUD write. Redis absorbs the hot traffic, Kafka decouples persistence and downstream consumers, and PostgreSQL remains the system of record for durable history."

### Dispatch engine

- ride request event triggers candidate search
- nearby drivers come from Redis first, PostGIS second
- weighted scoring considers distance, ETA, rating, acceptance, cancellation, idle time, risk penalty, vehicle compatibility, and availability
- assignment is concurrency-safe to prevent double assignment
- timeout and reassignment are supported

Interview version:

"Dispatch is the decision engine of the platform. I optimized it around short-lived candidate sets, fast proximity lookup, deterministic scoring, and safe state transitions to avoid race conditions when many drivers are being evaluated at once."

### WebSocket layer

- rider and driver updates are pushed over STOMP/WebSocket
- session registry tracks connection health
- driver location fanout guard prevents excessive publishes
- current scaling limit is the simple broker, which is acceptable early but not the final state

### Kafka consumers

- separate consumer groups for dispatch, realtime, fraud, and notifications
- natural future optimization is listener concurrency per topic plus DLQs and lag monitoring

### Fraud pipeline

- cancellation, payment, and tracking events are streamed to Kafka
- Redis counters back rolling rule windows
- fraud flags and risk profiles persist to PostgreSQL
- blocked status is pushed back into driver eligibility so risk actually changes business behavior

## Security Story

### Identity and access

- Keycloak is the identity provider
- Spring Boot acts as a JWT resource server
- frontend apps are designed around OIDC-compatible login flows
- roles include rider, driver, platform-admin, ops-admin, support-agent, and fraud-analyst

### Authorization

- endpoint-level RBAC protects rider, driver, and admin APIs
- domain-level authorization protects business operations
- sensitive admin actions are audited

### Abuse and safety controls

- Redis-backed rate limiting by actor class
- idempotency protection on booking and payment-like endpoints
- correlation IDs for traceability
- structured logging and audit records for privileged operations

Interview version:

"I didn’t treat auth as just login. The system uses identity, RBAC, rate limiting, idempotency, and auditability together so both normal user traffic and privileged operations are protected."

## Fraud Detection Story

### Why fraud matters

Ride-hailing is vulnerable to promo abuse, fake trips, GPS spoofing, rider-driver collusion, and cancellation gaming.

### Signals implemented

- repeated cancellations
- failed payment attempts
- suspicious short repeated trips
- GPS spoofing indicators
- route deviation anomalies
- account and device anomalies
- promo abuse windows

### Architecture

- Kafka listeners consume cancellation, payment, and location topics
- Redis stores short-lived counters and velocity windows
- PostgreSQL stores durable risk profiles and fraud flags
- admin and fraud-analyst dashboards surface alerts for review

### Business impact

- risky drivers can be excluded from dispatch
- risky riders can be blocked or put on hold
- fraud signals become part of operational decision-making, not just reporting

Interview version:

"The important thing is that fraud was not an offline report. I integrated the fraud score into live platform decisions like driver eligibility and administrative review, so the system could actively reduce platform loss."

## Shared Ride Matching Explanation

### Problem

Shared rides are much harder than normal rides because the system must keep both riders satisfied while optimizing route efficiency and seat usage.

### First version design

- evaluates whether a new rider can be inserted into an existing route
- supports the four main two-rider pickup/drop permutations:
  - pickup A / pickup B / drop A / drop B
  - pickup A / pickup B / drop B / drop A
  - pickup B / pickup A / drop A / drop B
  - pickup B / pickup A / drop B / drop A
- enforces:
  - max pickup wait time
  - max detour time
  - max detour percentage
  - seat capacity
  - rider compatibility threshold

### Why this is strong in interviews

It shows you did not hand-wave pooled rides. You modeled route feasibility, operational constraints, pricing implications, and extensibility for N riders later.

Interview version:

"I intentionally built pooled matching as a route-insertion problem with explicit constraint checks, not as a simple if-else on ride type. That made the first version realistic and left a clear path toward richer optimization later."

## How AI Improved The Product

The AI service is separate from the core backend so model evolution does not destabilize the transactional ride platform.

### Use cases

- fraud risk scoring
- ride cancellation probability
- driver acceptance probability
- demand hotspot prediction
- pooled ride compatibility scoring

### Product value

- fraud scoring reduces platform abuse and operational loss
- cancellation probability can improve dispatch or rider messaging
- driver acceptance probability improves assignment quality
- hotspot prediction can support surge and driver supply positioning
- compatibility scoring improves pooled match quality

Interview version:

"I used AI where probability actually improves product decisions, but I kept it outside the transaction-critical core. That gave me the benefit of ML without making the platform dependent on model latency for every request."

## Metrics You Can Realistically Claim

These should be framed as local or Docker Compose simulation results, not global production numbers.

### Safe, realistic claims

- "In local k6 simulations, the booking flow sustained tens of requests per second with sub-second p95 latency on a laptop-scale environment."
- "Driver location ingestion handled hundreds of updates per second with low hundreds of milliseconds p95 latency when Redis absorbed the hot path."
- "Realtime ride updates remained near real time under baseline simulation, with throttled fanout to avoid flooding subscribers."
- "Dispatch and fraud were event-driven, so booking latency stayed isolated from slower downstream work."

### Concrete numbers you can use if you run the provided tests and stay close to baseline

- booking:
  20 to 50 requests/sec
  p95 around 500 to 800 ms
  p99 under 1.5 s
- driver location:
  150 to 300 updates/sec
  p95 around 150 to 300 ms
  p99 under 700 ms
- AI inference:
  low tens of milliseconds to low hundreds depending on model and feature shape

### How to say this honestly

Good:

"In local Docker Compose and k6-based simulations, I validated the architecture at tens of booking requests per second and a few hundred driver location updates per second while keeping the hot path latency within practical targets."

Avoid:

"It supports millions of rides."

## Architecture Diagram Description You Can Draw In An Interview

Draw it left to right.

### Layer 1: Clients

- Rider App
- Driver App
- Admin Dashboard

All three point to the backend.

### Layer 2: Platform Edge

- Spring Boot Backend
- REST APIs
- WebSocket/STOMP for realtime
- Keycloak beside it for identity

Show Keycloak issuing JWTs to clients and backend validating them.

### Layer 3: Core Domain Modules Inside Backend

Inside one backend box, divide into:

- Rider
- Driver
- Ride
- Dispatch
- Shared Ride
- Pricing
- Fraud
- Notifications
- Admin

Mention modular monolith with clean domain boundaries.

### Layer 4: Data and Messaging

Below backend draw:

- PostgreSQL
- Redis
- Kafka

Arrows:

- Backend <-> PostgreSQL for transactional state
- Backend <-> Redis for hot state, geo, cache, rate limit, idempotency
- Backend -> Kafka for domain events
- Kafka -> dispatch, fraud, realtime, notifications consumers

### Layer 5: AI and Observability

To the right or below:

- FastAPI AI Service
- Prometheus
- Grafana
- Loki

Show backend calling AI service for scoring, and metrics/logs flowing into observability stack.

## Likely Interview Questions And Strong Answers

### 1. Why did you choose a modular monolith instead of microservices?

Strong answer:

"The most critical domains like dispatch, pricing, fraud, and ride lifecycle were tightly coupled and evolving quickly. A modular monolith let me preserve strong consistency and move fast without distributed transaction overhead. I still designed clear extraction seams through Kafka events, isolated domain packages, and dedicated infrastructure adapters."

### 2. How does dispatch choose the best driver?

Strong answer:

"The dispatch engine first finds nearby eligible drivers, primarily from Redis-backed current state and geospatial lookup. It then computes a weighted score using distance, ETA, rating, acceptance rate, cancellation rate, idle time, fraud penalty, vehicle compatibility, and availability. The assignment flow is concurrency-safe and supports timeout plus reassignment."

### 3. How did you handle real-time location at scale?

Strong answer:

"I separated hot mutable state from durable history. Current driver location lives in Redis GEO for low-latency search and fanout, while PostgreSQL is reserved for durable state and history. Location events are also published to Kafka so persistence, fraud analysis, and realtime delivery can scale independently."

### 4. What made pooled rides hard?

Strong answer:

"The core challenge was not just combining riders, but preserving user experience under route constraints. I modeled route insertion explicitly, evaluated key pickup/drop permutations, and enforced wait time, detour, and seat-capacity constraints. That made the first implementation realistic while keeping it extensible to more advanced optimization later."

### 5. How did you secure the platform?

Strong answer:

"I used Keycloak for OIDC and role-based access control, secured the backend as a JWT resource server, added role checks across rider, driver, and admin paths, and layered Redis-backed rate limiting plus idempotency for abuse and reliability. I also logged privileged admin actions for auditability."

### 6. How does fraud detection work?

Strong answer:

"Fraud is event driven. Ride cancellations, payments, and location updates go through Kafka. Redis maintains short-lived counters and rolling windows for velocity checks. PostgreSQL stores durable risk profiles and alerts. The result is not just passive reporting because fraud state feeds back into dispatch eligibility and admin workflows."

### 7. Where would this architecture break first at higher scale?

Strong answer:

"The first likely constraints are the in-memory WebSocket broker, dispatch coordination under surge, and if left unchecked, too many location writes hitting the database. My scaling plan is to keep Redis as the hot path, increase Kafka partition-aware concurrency, move more persistence async, and externalize realtime coordination when connection counts justify it."

### 8. How did AI add value without making the system fragile?

Strong answer:

"I kept AI as a separate FastAPI inference service rather than embedding it in the critical transaction path. That let the platform use probabilistic signals for fraud, cancellations, acceptance, and pooled compatibility while keeping the ride system resilient even if models are slow or being retrained."

### 9. What production-hardening steps did you add?

Strong answer:

"I added actuator health and readiness probes, structured JSON logging, Prometheus and Grafana metrics, centralized logging, Docker-based local orchestration, CI validation, performance-tuning profiles, and k6 scripts for the two hottest paths: booking and driver location ingestion."

### 10. If you had more time, what would you improve next?

Strong answer:

"I would add a transactional outbox, Kafka dead-letter handling, Redis-backed WebSocket presence, partitioning for large append-heavy PostgreSQL tables, and an external broker or realtime gateway once the in-process WebSocket broker became the limiting factor."

## Strong Closing Narrative

If the interviewer asks you to summarize the project in one minute, say this:

"I built a production-style ride-hailing platform with normal rides, pooled rides, real-time tracking, fraud detection, dynamic pricing, and admin operations. The backend is a modular monolith in Spring Boot with PostgreSQL for transactional state, Redis for hot state and geospatial lookup, Kafka for asynchronous workflows, Keycloak for auth, WebSocket for live updates, and a Python AI service for risk and probability-based intelligence. The part I am proudest of is that I designed it like a real platform, not just a CRUD app: dispatch, pooling, fraud, realtime, observability, and scaling were first-class concerns from the start."
