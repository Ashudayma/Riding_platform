ALTER TABLE notification.notification
    ADD COLUMN IF NOT EXISTS event_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS provider_key VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_notification_retry_queue
    ON notification.notification (delivery_status, next_retry_at, created_at)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_notification_event_code
    ON notification.notification (event_code, channel, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE notification.notification_template (
    id UUID PRIMARY KEY,
    event_code VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'EMAIL', 'IN_APP', 'WEBHOOK')),
    locale VARCHAR(16) NOT NULL DEFAULT 'en',
    title_template VARCHAR(255),
    body_template VARCHAR(4000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_notification_template_event_channel_locale UNIQUE (event_code, channel, locale)
);

CREATE INDEX idx_notification_template_lookup
    ON notification.notification_template (event_code, channel, locale, active)
    WHERE deleted_at IS NULL;

CREATE TABLE notification.user_notification_preference (
    id UUID PRIMARY KEY,
    user_profile_id UUID NOT NULL REFERENCES identity.user_profile (id),
    event_code VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'EMAIL', 'IN_APP', 'WEBHOOK')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_notification_preference_user_event_channel UNIQUE (user_profile_id, event_code, channel)
);

CREATE INDEX idx_notification_preference_user
    ON notification.user_notification_preference (user_profile_id, enabled, updated_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE notification.notification_delivery_attempt (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notification.notification (id),
    attempt_no INTEGER NOT NULL,
    provider_key VARCHAR(64),
    provider_status VARCHAR(32) NOT NULL CHECK (provider_status IN ('SENT', 'DELIVERED', 'FAILED', 'RETRY_SCHEDULED', 'SKIPPED')),
    response_payload_json JSONB,
    failure_reason VARCHAR(255),
    attempted_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_notification_attempt UNIQUE (notification_id, attempt_no)
);

CREATE INDEX idx_notification_attempt_notification
    ON notification.notification_delivery_attempt (notification_id, attempt_no DESC)
    WHERE deleted_at IS NULL;
