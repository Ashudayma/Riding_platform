ALTER TABLE driver.driver_availability
    ADD COLUMN available_since TIMESTAMPTZ;

UPDATE driver.driver_availability
SET available_since = COALESCE(last_heartbeat_at, updated_at)
WHERE availability_status = 'AVAILABLE'
  AND available_since IS NULL;

CREATE INDEX idx_driver_availability_available_since
    ON driver.driver_availability (availability_status, online_status, available_since)
    WHERE deleted_at IS NULL;

CREATE TABLE ride.driver_assignment_attempt (
    id UUID PRIMARY KEY,
    ride_request_id UUID NOT NULL REFERENCES ride.ride_request (id),
    ride_id UUID NOT NULL REFERENCES ride.ride (id),
    driver_profile_id UUID REFERENCES driver.driver_profile (id),
    vehicle_id UUID REFERENCES driver.vehicle (id),
    dispatch_round INTEGER NOT NULL CHECK (dispatch_round >= 1),
    attempt_no INTEGER NOT NULL CHECK (attempt_no >= 1),
    assignment_status VARCHAR(32) NOT NULL CHECK (
        assignment_status IN ('PENDING_DRIVER_RESPONSE', 'ACCEPTED', 'REJECTED', 'TIMED_OUT', 'FAILED', 'SKIPPED')
    ),
    failure_reason_code VARCHAR(64),
    weighted_score NUMERIC(10,6),
    distance_meters NUMERIC(10,2),
    eta_seconds INTEGER,
    score_breakdown_json JSONB,
    assignment_token UUID,
    offered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    responded_at TIMESTAMPTZ,
    response_note VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_driver_assignment_attempt_token UNIQUE (assignment_token)
);

CREATE UNIQUE INDEX uk_driver_assignment_attempt_order
    ON ride.driver_assignment_attempt (ride_request_id, attempt_no)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_driver_assignment_attempt_pending_expiry
    ON ride.driver_assignment_attempt (assignment_status, expires_at)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_driver_assignment_attempt_request_round
    ON ride.driver_assignment_attempt (ride_request_id, dispatch_round, attempt_no DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_driver_assignment_attempt_driver_status
    ON ride.driver_assignment_attempt (driver_profile_id, assignment_status, offered_at DESC)
    WHERE deleted_at IS NULL;
