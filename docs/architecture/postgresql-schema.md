# PostgreSQL Schema and JPA Model

## ERD-Style Explanation

This schema uses **separate PostgreSQL schemas per business domain** while preserving cross-schema foreign keys:

- `identity.user_profile`
  Root profile shadow table for Keycloak-backed identities. Every rider, driver, admin actor, reviewer, and notification recipient anchors here.
- `rider.rider_profile`
  One-to-one extension of `identity.user_profile` for rider-specific metrics and restrictions.
- `driver.driver_profile`
  One-to-one extension of `identity.user_profile` for driver-specific compliance, onboarding, quality, and risk metrics.
- `driver.vehicle`
  One-to-many from driver to vehicles, with a `current_vehicle_id` pointer on driver for the active vehicle.
- `driver.driver_availability`
  One-to-one mutable operational state for driver online/availability status and last known location.
- `payment.payment_method`
  One-to-many from rider to tokenized payment methods.
- `pricing.fare_quote`
  Price snapshot entity reused by both requests and final rides. `pricing.fare_breakdown_item` stores normalized line items.
- `ride.ride_request`
  The customer booking intent. Linked to rider, fare quote, payment method, origin/destination, and request status.
- `ride.ride`
  The executable/actual trip. Linked to the originating booking request, assigned driver and vehicle, optional shared ride group, and final fare quote.
- `ride.ride_stop`
  Ordered pickup/drop/waypoint rows. Each stop belongs to a request and can later be attached to a concrete ride, which enables pre-match and post-match route ordering.
- `sharedride.shared_ride_group`
  Active or historical pooled trip container.
- `sharedride.shared_ride_candidate`
  Compatibility evaluation between two ride requests, optionally tied to a proposed group.
- `tracking.driver_location_history`
  Append-only geospatial telemetry for driver movements and trip replay/debugging.
- `ride.ride_status_history`
  Append-only lifecycle history for both ride requests and actual rides.
- `payment.payment_transaction`
  Provider-agnostic payment transaction ledger covering auth/capture/refund/void flows.
- `fraud.fraud_flag`
  Generic fraud/risk signal table keyed by `subject_type` + `subject_id`.
- `rating.rating_review`
  Bidirectional post-ride feedback. Supports rider->driver and driver->rider reviews.
- `notification.notification`
  Delivery-tracked outbound/in-app notification records.
- `admin.admin_audit_log`
  Immutable admin/operator audit events.

## Design Choices

- `UUID` is used consistently for primary keys and cross-table references.
- `version` exists on mutable aggregates for optimistic locking.
- `deleted_at` is used instead of hard delete for user-facing/business entities that may require recovery or auditing.
- Append-heavy history tables (`driver_location_history`, `ride_status_history`, `admin_audit_log`) are modeled so they can later be **partitioned by month** without changing application code.
- Geospatial columns use `PostGIS geography(Point,4326)` for driver proximity and stop/request coordinates.
- PostgreSQL `CHECK` constraints are used for enum-like state columns instead of PostgreSQL enum types to keep deployments and future value additions safer.

## Main Relationship Summary

- One `user_profile` can back one `rider_profile` and/or one `driver_profile`.
- One `driver_profile` can own many `vehicle` rows.
- One `driver_profile` has one mutable `driver_availability` row.
- One `rider_profile` can create many `ride_request` rows.
- One `ride_request` can produce one `ride` in standard flow, but multiple requests can attach to the same shared `ride`.
- One `ride_request` has many `ride_stop` rows in request order.
- One `ride` has many `ride_stop` rows in actual execution order.
- One `shared_ride_group` groups pooled requests and may be linked to one concrete shared `ride`.
- One `fare_quote` can have many `fare_breakdown_item` rows.
- One `ride` can have many `payment_transaction`, `notification`, `rating_review`, and `ride_status_history` rows.

## Scalability Notes

- Add monthly partitions for `tracking.driver_location_history`, `ride.ride_status_history`, and `admin.admin_audit_log` once data volumes grow.
- Keep nearby-driver search in Redis for hot path reads, while PostgreSQL/PostGIS remains the durable and analytical source.
- Add BRIN indexes later on timestamp-heavy history tables if retention becomes large.
- For shared ride optimization, `shared_ride_candidate` is intentionally normalized enough to support both synchronous heuristics and async scoring pipelines.
