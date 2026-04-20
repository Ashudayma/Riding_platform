# DevOps And Observability

This document captures the local-first DevOps baseline for the riding platform and the production recommendations that should guide the next deployment phase.

## Runtime Packaging

### Containers

- Backend: multi-stage Maven build in [backend/ride-platform-app/Dockerfile](/d:/Riding_Platform/backend/ride-platform-app/Dockerfile)
- Frontend apps: workspace-aware Next.js build in [frontend/Dockerfile](/d:/Riding_Platform/frontend/Dockerfile)
- AI service: FastAPI container in [ai-services/risk-scoring/Dockerfile](/d:/Riding_Platform/ai-services/risk-scoring/Dockerfile)

### Local stack

The local platform stack is orchestrated from [infra/docker/docker-compose.yml](/d:/Riding_Platform/infra/docker/docker-compose.yml) and includes:

- PostgreSQL + PostGIS
- Redis
- Kafka
- Keycloak
- Spring Boot backend
- FastAPI AI risk service
- rider, driver, and admin web apps
- Prometheus
- Grafana
- Loki
- Promtail

## Environment Variable Strategy

Use three layers of configuration:

1. Code defaults for developer convenience
2. `.env` or compose-level environment variables for local orchestration
3. production secret injection through the deployment platform

Reference templates:

- root example: [.env.example](/d:/Riding_Platform/.env.example)
- compose example: [infra/docker/.env.example](/d:/Riding_Platform/infra/docker/.env.example)

### Recommended variable groups

- Infrastructure:
  - `POSTGRES_*`
  - `REDIS_PORT`
  - `KAFKA_PORT`
  - `KEYCLOAK_*`
- Backend:
  - `SPRING_DATASOURCE_*`
  - `SPRING_DATA_REDIS_*`
  - `SPRING_KAFKA_BOOTSTRAP_SERVERS`
  - `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`
  - `PLATFORM_*`
  - `JAVA_OPTS`
- Frontend:
  - `NEXT_PUBLIC_API_BASE_URL`
  - `NEXT_PUBLIC_WS_URL`
  - `NEXT_PUBLIC_KEYCLOAK_*`
- AI service:
  - `AI_SERVICE_LOG_LEVEL`

### Secret handling

Never commit real credentials for:

- database passwords
- Keycloak admin password
- JWT client secrets
- Grafana admin credentials
- production API keys

Use secret stores in production:

- GitHub Actions secrets
- cloud secret manager
- Kubernetes secrets sealed or externally managed

## Health, Readiness, And Liveness

### Backend

The backend now exposes:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`
- `/actuator/prometheus`

Readiness includes:

- db
- redis
- ping

Liveness includes:

- liveness state
- ping

### AI service

The AI service exposes:

- `/api/v1/health`
- `/metrics`

### Containers

Docker Compose health checks are configured for:

- postgres
- redis
- kafka
- keycloak
- backend
- ai-risk-scoring
- rider-web
- driver-web
- admin-web
- prometheus
- loki
- grafana

## Metrics Integration

### Backend metrics

Spring Boot Actuator + Micrometer Prometheus registry are enabled. The service is tagged with:

- `application`
- `environment`
- `platform`

### AI service metrics

FastAPI metrics are exposed with `prometheus-fastapi-instrumentator`.

### Prometheus

Prometheus scrape config lives at:

- [infra/observability/prometheus/prometheus.yml](/d:/Riding_Platform/infra/observability/prometheus/prometheus.yml)

Current scrape targets:

- Prometheus self metrics
- Spring Boot backend
- AI service
- Keycloak

## Structured Logging

### Backend

Structured JSON logging is configured in:

- [backend/ride-platform-app/src/main/resources/logback-spring.xml](/d:/Riding_Platform/backend/ride-platform-app/src/main/resources/logback-spring.xml)

The backend includes:

- JSON logs to stdout
- JSON rolling file logs
- correlation id propagation through MDC `requestId`

### AI service

Structured JSON logging is configured in:

- [ai-services/risk-scoring/app/core/logging_config.py](/d:/Riding_Platform/ai-services/risk-scoring/app/core/logging_config.py)
- [ai-services/risk-scoring/app/core/middleware.py](/d:/Riding_Platform/ai-services/risk-scoring/app/core/middleware.py)

The AI service includes:

- JSON stdout logging
- correlation id propagation
- request completion logging with latency

### Centralized logging

Local centralized logging is provided with:

- Loki
- Promtail via Docker service discovery
- Grafana log exploration

Promtail config:

- [infra/observability/promtail/promtail-config.yml](/d:/Riding_Platform/infra/observability/promtail/promtail-config.yml)

Loki config:

- [infra/observability/loki/loki-config.yml](/d:/Riding_Platform/infra/observability/loki/loki-config.yml)

## Grafana Dashboard Suggestions

Provisioning files:

- datasource config: [infra/observability/grafana/provisioning/datasources/datasources.yml](/d:/Riding_Platform/infra/observability/grafana/provisioning/datasources/datasources.yml)
- dashboard provider: [infra/observability/grafana/provisioning/dashboards/dashboards.yml](/d:/Riding_Platform/infra/observability/grafana/provisioning/dashboards/dashboards.yml)
- starter dashboard: [infra/observability/grafana/dashboards/platform-overview.json](/d:/Riding_Platform/infra/observability/grafana/dashboards/platform-overview.json)

Recommended production dashboards:

- API latency:
  - p50, p95, p99 by endpoint and HTTP method
- Ride lifecycle:
  - requested, searching, assigned, in-progress, completed, cancelled throughput
- Dispatch:
  - assignment success, timeout, rejection, average rounds to assign
- Shared ride:
  - average seat utilization, pooling savings, detour acceptance rate
- Fraud:
  - alerts opened, severity distribution, blocks triggered
- Kafka:
  - consumer lag by topic and consumer group
- Infrastructure:
  - JVM heap, CPU, DB connections, Redis memory, container restarts
- Log dashboards:
  - error volume by service
  - top exception classes
  - request correlation log drilldown

## CI/CD

GitHub Actions pipeline:

- [platform-ci.yml](/d:/Riding_Platform/.github/workflows/platform-ci.yml)

Current stages:

- backend test and package
- frontend dependency install and typecheck
- AI dependency install and app smoke import
- docker compose configuration validation

### Recommended production CD stages

1. Build immutable images tagged with git SHA and semantic version.
2. Run SBOM generation and container vulnerability scanning.
3. Push images to a private registry.
4. Deploy first to staging.
5. Run smoke tests against staging readiness endpoints.
6. Promote to production with approval gates.

## Production Deployment Recommendations

### Platform choice

For production, prefer Kubernetes or a managed container platform over long-lived Docker Compose.

Recommended split:

- managed PostgreSQL
- managed Redis
- managed Kafka
- managed Keycloak or identity service
- container platform for backend, frontend, and AI service

### Deployment topology

- Frontends behind CDN or reverse proxy
- Backend behind API gateway / ingress
- AI service private to the backend network
- Prometheus remote-write or managed metrics where appropriate
- Loki or OpenSearch cluster for logs

### Reliability and scaling

- backend:
  - multiple replicas
  - horizontal scaling on CPU and request rate
  - graceful shutdown and rolling deployment
- frontend:
  - edge caching for static assets
  - separate deployments per app if traffic differs
- AI service:
  - independent autoscaling
  - model artifact version pinning
- Kafka:
  - explicit topic retention and partition strategy
- Postgres:
  - backups, WAL archiving, connection pooling

### Security

- terminate TLS at ingress
- restrict actuator and metrics with network policy in production
- run non-root containers where possible
- use image signing and registry scanning
- rotate secrets centrally

### Next recommended upgrades

1. Add OpenTelemetry tracing for Spring Boot and FastAPI.
2. Add Kafka lag exporter and Redis exporter.
3. Add alerting rules for readiness failures, error spikes, and queue lag.
4. Split staging and production compose-independent manifests.
