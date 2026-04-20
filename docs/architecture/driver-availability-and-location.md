# Driver Availability and Live Location Design

## API Surface

- `PATCH /api/v1/driver/availability`
  Driver goes online or offline and updates session/app metadata.
- `POST /api/v1/driver/location`
  Driver updates current live location.
- `POST /api/v1/driver/search/nearby`
  Internal operational search for nearby drivers with filters.

## Redis Data Model

- GEO key:
  `drivers:geo:all`
  Member: `driverProfileId`
  Coordinates: current longitude/latitude

- HASH per driver:
  `drivers:state:{driverProfileId}`
  Fields:
  - `driverProfileId`
  - `vehicleId`
  - `vehicleType`
  - `availabilityStatus`
  - `onlineStatus`
  - `currentRideId`
  - `averageRating`
  - `riskBlocked`
  - `latitude`
  - `longitude`
  - `lastHeartbeatAt`

This makes nearby search Redis-first and filter-friendly without reading PostgreSQL on every hot lookup.

## PostgreSQL / PostGIS Strategy

- `driver.driver_availability`
  Durable mutable snapshot of current driver state.
- `tracking.driver_location_history`
  Append-only durable location history.
- PostGIS `geography(Point,4326)` on both current availability and history.
- GIST indexes support fallback geo search and analytical queries.

## Nearby Search Strategy

1. Query Redis GEO for nearby driver IDs.
2. Read driver hashes from Redis and apply fast filters:
   - availability
   - vehicle type
   - active ride
   - minimum rating
   - risk blocked
3. If Redis is cold or results are insufficient, fall back to PostgreSQL/PostGIS.

## Background Processing

- Live location API updates current state synchronously:
  - PostgreSQL availability snapshot
  - Redis GEO/hash hot state
- Historical persistence is asynchronous:
  - publish Kafka event `riding-platform.tracking.location-updated`
  - consumer persists `tracking.driver_location_history`

This keeps the driver app request fast while still preserving durable telemetry.

## Indexing Strategy

- PostgreSQL:
  - `idx_driver_availability_state`
  - `idx_driver_availability_last_location_gist`
  - `idx_driver_location_history_driver_captured_at`
  - `idx_driver_location_history_ride_captured_at`
  - `idx_driver_location_history_location_gist`
- Redis:
  - native GEO indexing for proximity
  - O(1) hash lookup for state filters

## Scaling to a Large City

For a large city with many concurrent drivers:

- Keep current-state lookups entirely Redis-first.
- Use short-lived hot state with frequent heartbeat refresh.
- Partition driver population logically by city/zone in future:
  - `drivers:geo:{city}`
  - `drivers:geo:{zone}`
- Move from one general GEO key to sharded GEO keys when density grows.
- Keep Kafka-backed location history async so write latency stays flat even as telemetry volume rises.
- Partition `tracking.driver_location_history` by date/month when retention grows.
- Add grid-based search or H3/geohash routing later if dispatch fanout becomes very large.

This design scales well because the hot path is:

- one Redis GEO write
- one Redis hash write
- one PostgreSQL availability update
- one Kafka publish

That is predictable and cheap compared with synchronous historical inserts and relational geo joins on every location update.
