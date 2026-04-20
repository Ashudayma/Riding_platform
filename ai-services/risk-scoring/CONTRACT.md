# Java Integration Contract

This document defines how the Spring Boot backend should call the FastAPI AI service safely.

## Transport

- Protocol: HTTP/JSON
- Network scope: private internal network only
- Auth: internal service credential, gateway token, mTLS, or mesh identity
- Timeout: `300-800ms` depending on use case criticality
- Retry: only for safe inference calls, and at most once

## Request Headers

- `Content-Type: application/json`
- `Accept: application/json`
- `X-Correlation-Id: <trace-id>`
- `X-Request-Source: ride-platform-backend`

## Response Contract

Classification endpoints return:

```json
{
  "use_case": "fraud_risk",
  "model_version": "baseline-20260418123000",
  "score": 0.8421,
  "label": "HIGH_RISK",
  "threshold": 0.6,
  "accepted": true,
  "explanation": [
    "failed payment activity is elevated",
    "GPS spoofing indicators were observed recently"
  ]
}
```

Hotspot prediction returns:

```json
{
  "use_case": "demand_hotspot_prediction",
  "model_version": "baseline-20260418123000",
  "predictions": [
    {
      "zone_id": "ZONE-014",
      "hotspot_score": 0.9134,
      "label": "HOTSPOT",
      "explanation": [
        "recent requests exceed available drivers"
      ]
    }
  ]
}
```

## Failure Handling in Spring Boot

The ML service should not become a single point of failure for core ride flows.

- Fraud scoring: fall back to rule-engine score already available in backend.
- Cancellation prediction: fall back to heuristic based on ETA + surge + rider history.
- Driver acceptance prediction: fall back to deterministic dispatch score.
- Hotspot prediction: fall back to aggregated demand metrics from Redis/PostgreSQL.
- Pooling compatibility: fall back to rule-based compatibility score.

## Safe Java Client Design

Recommended client stack:

- Spring WebClient
- short connect/read timeout
- circuit breaker with Resilience4j
- structured logging with correlation id

Recommended persistence from Java:

- model version used
- returned score
- final accepted label
- request correlation id
- endpoint name

Do not store the full raw payload unless required for compliance or offline training workflows.

## Suggested DTO Mapping

Map backend domain objects to ML contracts only after:

- validation is complete
- IDs are resolved
- sensitive unnecessary fields are removed

Prefer sending compact derived features rather than raw event streams for synchronous inference.
