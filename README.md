# Riding Platform Monorepo

Production-grade foundation for a scalable ride-hailing and ride-sharing platform inspired by Uber, with shared-ride optimization and fraud/risk detection.

## Stack

- Java 21
- Spring Boot 3.x
- PostgreSQL
- Redis
- Kafka
- Keycloak
- WebSocket
- Docker / Docker Compose
- React or Next.js frontend
- Prometheus / Grafana
- ELK or OpenSearch

## Repository Layout

```text
.
|-- backend/
|   |-- pom.xml
|   `-- ride-platform-app/
|       |-- pom.xml
|       `-- src/
|           |-- main/
|           |   |-- java/com/ridingplatform/
|           |   `-- resources/
|           `-- test/
|-- frontend/
|   `-- README.md
|-- infra/
|   |-- docker/
|   |   |-- docker-compose.yml
|   |   `-- postgres/
|   |       `-- init/01-init-databases.sql
|   `-- observability/
|       `-- README.md
|-- docs/
|   `-- architecture/
|       `-- system-architecture.md
`-- ai-services/
    `-- risk-scoring/
        `-- README.md
```

## Architecture Decision

Start as a modular monolith, not as day-one microservices.

Why:

- Dispatch, pooling, pricing, fraud, and trip lifecycle have tightly coupled consistency needs in the early phase.
- A modular monolith keeps transactional boundaries simpler while the core domain is still evolving.
- You still get production-grade separation by enforcing package/module boundaries, async events, outbox-ready integration, and clear domain ownership.
- The chosen structure makes future extraction straightforward once traffic, team size, and operational complexity justify it.

## Quick Start

### 1. Start infrastructure

From [infra/docker/docker-compose.yml](/d:/Riding_Platform/infra/docker/docker-compose.yml):

```powershell
docker compose up -d
```

### 2. Create Keycloak realm and clients

- Open `http://localhost:8081`
- Login with `admin / admin`
- Create realm `riding-platform`
- Create clients:
  - `riding-platform-api`
  - `riding-platform-admin`
  - `riding-platform-web`
- Create realm roles:
  - `platform-admin`
  - `ops-admin`
  - `rider`
  - `driver`
  - `fraud-analyst`
  - `support-agent`

### 3. Run the backend

From `backend/ride-platform-app`:

```powershell
mvn spring-boot:run
```

### 4. Health check

```text
GET http://localhost:8080/api/v1/system/health
```

## What Is Included

- Production-style architecture documentation
- Modular monolith Spring Boot skeleton with clean architecture layering
- Domain-aligned packages for rider, driver, ride, shared ride, dispatch, pricing, tracking, fraud, and admin
- Security configuration ready for Keycloak JWT resource-server integration
- WebSocket and Kafka-ready configuration scaffolding
- Docker Compose for PostgreSQL, Redis, Kafka, and Keycloak
- Environment-driven configuration for local development
- Containerization, CI, metrics, Grafana, and centralized logging foundation

## Recommended Next Build Steps

1. Implement Flyway migrations for core tables.
2. Add transactional outbox and Kafka publisher.
3. Build ride request, dispatch, and driver availability flows.
4. Add location ingestion and WebSocket streaming.
5. Integrate pricing and fraud scoring into booking and assignment.
6. Add frontend apps for rider, driver, and admin use cases.

## Reference Docs

- Architecture: [docs/architecture/system-architecture.md](/d:/Riding_Platform/docs/architecture/system-architecture.md)
- Backend application entrypoint: [RidingPlatformApplication.java](/d:/Riding_Platform/backend/ride-platform-app/src/main/java/com/ridingplatform/RidingPlatformApplication.java)
- Compose stack: [docker-compose.yml](/d:/Riding_Platform/infra/docker/docker-compose.yml)
- DevOps and observability: [docs/operations/devops-observability.md](/d:/Riding_Platform/docs/operations/devops-observability.md)
- Production hardening and scaling: [docs/operations/production-hardening-and-scaling.md](/d:/Riding_Platform/docs/operations/production-hardening-and-scaling.md)
