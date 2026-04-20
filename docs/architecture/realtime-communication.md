# Realtime Communication Layer

## Architecture

The platform now uses Spring WebSocket + STOMP for first-party realtime delivery with a
user-destination-first model.

Client connection:

- STOMP endpoint: `/ws`
- SockJS enabled for browser compatibility and reconnect fallback
- JWT passed in the STOMP `CONNECT` frame via `Authorization: Bearer <token>`
- JWT decoded with the same Keycloak resource-server stack used by HTTP APIs

Delivery model:

- rider ride updates: `/user/queue/rides`
- rider live driver location: `/user/queue/ride-location`
- driver assignment updates: `/user/queue/assignments`
- driver ride lifecycle updates: `/user/queue/driver-rides`
- session health/state: `/user/queue/session-state`

## Security

Security is enforced at connect time by `StompPrincipalChannelInterceptor`.

- only authenticated `CONNECT` frames are accepted
- socket principal is derived from the JWT `sub`
- user destinations map directly to Keycloak subject IDs
- no ride data is broadcast on public topics in this version

This avoids leaking ride or location data across subscribers and keeps authorization simple.

## Event Flow

The websocket layer is event-driven rather than request-driven.

Sources:

- ride lifecycle Kafka topics
- dispatch Kafka topics
- driver location Kafka topic

Fanout:

- `RideRealtimeEventListener` consumes Kafka events
- `RideRealtimeGateway` resolves rider/driver websocket audiences from ride data
- `StompRealtimeMessagingService` pushes frames to user queues

## High-Frequency Location Optimization

Driver location fanout is protected by `DriverLocationFanoutGuard`.

- publishes only for active rides
- throttles outbound frames with Redis
- suppresses very small location deltas
- avoids database writes for socket delivery

Persistent location history still flows through Kafka into the tracking store separately, so realtime delivery
does not increase write pressure on PostgreSQL.

## Reconnects And Stale Sessions

- broker heartbeats are enabled
- `/app/session/ping` lets clients refresh liveness explicitly
- `WebSocketSessionRegistry` tracks connect/disconnect/last-seen state
- stale sessions are evicted on a schedule and clients receive a state event

## Scaling Guidance

For a city-scale production deployment, the current design should evolve in this order:

1. Replace the in-process simple broker with a STOMP broker relay or dedicated pub/sub backbone.
2. Run websocket nodes stateless behind a load balancer with sticky sessions only if required by the chosen broker.
3. Keep Kafka as the authoritative event bus for ride/dispatch/location state.
4. Use Redis only for throttling, ephemeral session hints, and lightweight realtime coordination.
5. Partition ride/location topics by city or region for predictable consumer scaling.

## Production Concerns

- Connection floods:
  Protect the `/ws` endpoint behind gateway rate limits and WAF rules.

- Broker memory pressure:
  Keep payloads compact and prefer user queues over large shared topics.

- Event ordering:
  Preserve ordering per ride by keying Kafka messages with ride request or ride ID.

- Backpressure:
  Throttle location fanout aggressively and degrade gracefully before touching persistence paths.

- Horizontal scale:
  Move from `enableSimpleBroker` to a broker relay for many concurrent connections.
