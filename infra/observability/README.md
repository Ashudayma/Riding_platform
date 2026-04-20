# Observability

Local observability assets now live in this directory:

- Prometheus scrape configuration
- Grafana datasource and dashboard provisioning
- Loki log storage configuration
- Promtail Docker log shipping configuration

Primary files:

- Prometheus: [prometheus/prometheus.yml](/d:/Riding_Platform/infra/observability/prometheus/prometheus.yml)
- Loki: [loki/loki-config.yml](/d:/Riding_Platform/infra/observability/loki/loki-config.yml)
- Promtail: [promtail/promtail-config.yml](/d:/Riding_Platform/infra/observability/promtail/promtail-config.yml)
- Grafana provisioning: [grafana/provisioning](/d:/Riding_Platform/infra/observability/grafana/provisioning)
- Starter dashboard: [grafana/dashboards/platform-overview.json](/d:/Riding_Platform/infra/observability/grafana/dashboards/platform-overview.json)

For the broader DevOps and production deployment strategy, see:

- [docs/operations/devops-observability.md](/d:/Riding_Platform/docs/operations/devops-observability.md)
