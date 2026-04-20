# Load Testing

This directory contains k6-based load tests for the platform's hottest execution paths.

## Scenarios

- [k6/booking-flow.js](/d:/Riding_Platform/infra/loadtest/k6/booking-flow.js)
  Estimates fare and creates ride bookings using an idempotency key.
- [k6/driver-location-updates.js](/d:/Riding_Platform/infra/loadtest/k6/driver-location-updates.js)
  Exercises high-volume driver location ingestion.

## Running k6 locally

### Booking flow

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e ACCESS_TOKEN=<rider-access-token> `
  infra/loadtest/k6/booking-flow.js
```

### Driver location updates

```powershell
k6 run `
  -e BASE_URL=http://localhost:8080 `
  -e ACCESS_TOKEN=<driver-access-token> `
  infra/loadtest/k6/driver-location-updates.js
```

## Recommended test progression

1. Smoke:
   - 1 to 5 RPS booking
   - 20 to 50 updates/sec driver location
2. Baseline:
   - 20 to 50 booking requests/sec
   - 200 updates/sec driver location
3. Stress:
   - increase until p95 or error thresholds breach
4. Soak:
   - run 30 to 60 minutes at expected peak load

## What to watch during tests

- backend p95 and p99 latency
- PostgreSQL CPU and connection usage
- Redis ops/sec, latency, and memory
- Kafka consumer lag
- WebSocket session counts
- dispatch timeout rate
- fraud evaluation backlog
