CREATE TABLE sharedride.shared_ride_route_stop (
    id UUID PRIMARY KEY,
    shared_ride_group_id UUID NOT NULL REFERENCES sharedride.shared_ride_group (id),
    ride_request_id UUID NOT NULL REFERENCES ride.ride_request (id),
    rider_profile_id UUID NOT NULL REFERENCES rider.rider_profile (id),
    stop_type VARCHAR(32) NOT NULL CHECK (stop_type IN ('PICKUP', 'DROPOFF', 'WAYPOINT')),
    route_sequence_no INTEGER NOT NULL CHECK (route_sequence_no >= 1),
    stop_point GEOGRAPHY(Point, 4326) NOT NULL,
    address_line VARCHAR(500) NOT NULL,
    locality VARCHAR(120),
    passenger_count SMALLINT NOT NULL DEFAULT 1 CHECK (passenger_count BETWEEN 1 AND 6),
    planned_arrival_at TIMESTAMPTZ,
    detour_seconds INTEGER,
    metadata_json JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_shared_ride_route_stop_group_sequence
    ON sharedride.shared_ride_route_stop (shared_ride_group_id, route_sequence_no)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_shared_ride_route_stop_group_request
    ON sharedride.shared_ride_route_stop (shared_ride_group_id, ride_request_id, route_sequence_no)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_shared_ride_route_stop_point_gist
    ON sharedride.shared_ride_route_stop USING GIST (stop_point);
