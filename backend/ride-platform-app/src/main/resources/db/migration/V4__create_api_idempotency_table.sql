CREATE TABLE public.api_idempotency_key (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    actor_subject VARCHAR(128) NOT NULL,
    request_method VARCHAR(16) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    request_hash VARCHAR(128) NOT NULL,
    request_fingerprint_json JSONB,
    processing_status VARCHAR(32) NOT NULL CHECK (processing_status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    response_status INTEGER,
    response_body TEXT,
    resource_type VARCHAR(64),
    resource_id UUID,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_api_idempotency_actor_key UNIQUE (actor_subject, idempotency_key)
);

CREATE INDEX idx_api_idempotency_expires_at
    ON public.api_idempotency_key (expires_at);

CREATE INDEX idx_api_idempotency_request_path
    ON public.api_idempotency_key (request_path, created_at DESC);
