ALTER TABLE driver.driver_profile
    ADD COLUMN IF NOT EXISTS fraud_blocked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_driver_profile_fraud_blocked
    ON driver.driver_profile (fraud_blocked, risk_score, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE fraud.risk_profile (
    id UUID PRIMARY KEY,
    subject_type VARCHAR(32) NOT NULL CHECK (subject_type IN ('USER', 'RIDER', 'DRIVER', 'RIDE_REQUEST', 'RIDE', 'PAYMENT')),
    subject_id UUID NOT NULL,
    aggregate_score NUMERIC(8,4) NOT NULL DEFAULT 0.0000,
    risk_level VARCHAR(32) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    active_flag_count INTEGER NOT NULL DEFAULT 0,
    derived_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    manual_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked BOOLEAN NOT NULL DEFAULT FALSE,
    blocked_reason VARCHAR(255),
    last_signal_at TIMESTAMPTZ,
    last_assessed_at TIMESTAMPTZ,
    metadata_json JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_risk_profile_subject UNIQUE (subject_type, subject_id)
);

CREATE INDEX idx_risk_profile_blocked_level
    ON fraud.risk_profile (blocked, risk_level, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_risk_profile_subject_lookup
    ON fraud.risk_profile (subject_type, subject_id, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE fraud.fraud_signal_event (
    id UUID PRIMARY KEY,
    signal_type VARCHAR(64) NOT NULL,
    subject_type VARCHAR(32) NOT NULL CHECK (subject_type IN ('USER', 'RIDER', 'DRIVER', 'RIDE_REQUEST', 'RIDE', 'PAYMENT')),
    subject_id UUID NOT NULL,
    rider_profile_id UUID REFERENCES rider.rider_profile (id),
    driver_profile_id UUID REFERENCES driver.driver_profile (id),
    ride_request_id UUID REFERENCES ride.ride_request (id),
    ride_id UUID REFERENCES ride.ride (id),
    payment_transaction_id UUID REFERENCES payment.payment_transaction (id),
    source_topic VARCHAR(160),
    event_key VARCHAR(128),
    attributes_json JSONB,
    triggered_rules_json JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_fraud_signal_event_subject_occurred
    ON fraud.fraud_signal_event (subject_type, subject_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_fraud_signal_event_ride_occurred
    ON fraud.fraud_signal_event (ride_id, ride_request_id, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE fraud.fraud_review_action (
    id UUID PRIMARY KEY,
    fraud_flag_id UUID NOT NULL REFERENCES fraud.fraud_flag (id),
    actor_user_profile_id UUID REFERENCES identity.user_profile (id),
    action_type VARCHAR(32) NOT NULL CHECK (action_type IN ('UNDER_REVIEW', 'CONFIRM', 'DISMISS', 'RESOLVE', 'MANUAL_BLOCK', 'MANUAL_UNBLOCK', 'NOTE')),
    note VARCHAR(1000),
    metadata_json JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_fraud_review_action_flag_occurred
    ON fraud.fraud_review_action (fraud_flag_id, occurred_at DESC)
    WHERE deleted_at IS NULL;
