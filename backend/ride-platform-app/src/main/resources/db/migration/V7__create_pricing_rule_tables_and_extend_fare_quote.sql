CREATE TABLE pricing.pricing_rule_set (
    id UUID PRIMARY KEY,
    city_code VARCHAR(32) NOT NULL,
    zone_code VARCHAR(64),
    ride_type VARCHAR(32) NOT NULL CHECK (ride_type IN ('STANDARD', 'SHARED')),
    vehicle_type VARCHAR(32),
    pricing_version INTEGER NOT NULL CHECK (pricing_version >= 1),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    currency_code CHAR(3) NOT NULL,
    base_fare NUMERIC(12,2) NOT NULL DEFAULT 0,
    minimum_fare NUMERIC(12,2) NOT NULL DEFAULT 0,
    booking_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    per_km_rate NUMERIC(12,4) NOT NULL DEFAULT 0,
    per_minute_rate NUMERIC(12,4) NOT NULL DEFAULT 0,
    per_stop_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    waiting_charge_per_minute NUMERIC(12,4) NOT NULL DEFAULT 0,
    cancellation_base_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    cancellation_per_km_charge NUMERIC(12,4) NOT NULL DEFAULT 0,
    shared_discount_factor NUMERIC(8,4) NOT NULL DEFAULT 0,
    tax_percentage NUMERIC(8,4) NOT NULL DEFAULT 0,
    surge_cap_multiplier NUMERIC(8,4) NOT NULL DEFAULT 1.0000,
    night_surcharge_percentage NUMERIC(8,4) NOT NULL DEFAULT 0,
    airport_surcharge_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    created_by VARCHAR(100),
    metadata_json JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_pricing_rule_set_active_scope
    ON pricing.pricing_rule_set (city_code, COALESCE(zone_code, ''), ride_type, COALESCE(vehicle_type, ''), pricing_version)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_pricing_rule_set_lookup
    ON pricing.pricing_rule_set (city_code, zone_code, ride_type, vehicle_type, active, effective_from DESC)
    WHERE deleted_at IS NULL;

ALTER TABLE pricing.fare_quote
    ADD COLUMN pricing_rule_set_id UUID REFERENCES pricing.pricing_rule_set (id),
    ADD COLUMN city_code VARCHAR(32),
    ADD COLUMN zone_code VARCHAR(64),
    ADD COLUMN pricing_version INTEGER,
    ADD COLUMN waiting_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN cancellation_charge NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN finalized_distance_meters INTEGER,
    ADD COLUMN finalized_duration_seconds INTEGER,
    ADD COLUMN finalized_at TIMESTAMPTZ;

CREATE INDEX idx_fare_quote_rule_set
    ON pricing.fare_quote (pricing_rule_set_id, pricing_status, created_at DESC)
    WHERE deleted_at IS NULL;
