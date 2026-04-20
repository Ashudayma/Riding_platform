CREATE TABLE identity.user_profile (
    id UUID PRIMARY KEY,
    keycloak_user_id UUID NOT NULL,
    user_status VARCHAR(32) NOT NULL CHECK (user_status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'DISABLED')),
    email VARCHAR(320) NOT NULL,
    phone_country_code VARCHAR(8),
    phone_number VARCHAR(32),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    display_name VARCHAR(150),
    profile_photo_url VARCHAR(500),
    country_code VARCHAR(3),
    timezone VARCHAR(64),
    default_locale VARCHAR(16),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMPTZ,
    metadata_json JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_user_profile_keycloak_user_id_active
    ON identity.user_profile (keycloak_user_id)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_user_profile_email_active
    ON identity.user_profile (LOWER(email))
    WHERE deleted_at IS NULL;
CREATE INDEX idx_user_profile_status_created_at
    ON identity.user_profile (user_status, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE rider.rider_profile (
    id UUID PRIMARY KEY,
    user_profile_id UUID NOT NULL REFERENCES identity.user_profile (id),
    rider_code VARCHAR(32) NOT NULL,
    rider_status VARCHAR(32) NOT NULL CHECK (rider_status IN ('ACTIVE', 'RESTRICTED', 'BLOCKED')),
    average_rating NUMERIC(3,2) NOT NULL DEFAULT 5.00,
    lifetime_ride_count INTEGER NOT NULL DEFAULT 0,
    cancellation_count INTEGER NOT NULL DEFAULT 0,
    no_show_count INTEGER NOT NULL DEFAULT 0,
    fraud_hold BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_language VARCHAR(16),
    accessibility_requirements VARCHAR(255),
    emergency_contact_name VARCHAR(120),
    emergency_contact_phone VARCHAR(32),
    notes VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_rider_profile_user_profile UNIQUE (user_profile_id),
    CONSTRAINT uk_rider_profile_rider_code UNIQUE (rider_code)
);

CREATE INDEX idx_rider_profile_status
    ON rider.rider_profile (rider_status, fraud_hold)
    WHERE deleted_at IS NULL;

CREATE TABLE driver.driver_profile (
    id UUID PRIMARY KEY,
    user_profile_id UUID NOT NULL REFERENCES identity.user_profile (id),
    driver_code VARCHAR(32) NOT NULL,
    driver_status VARCHAR(32) NOT NULL CHECK (driver_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'BLOCKED')),
    onboarding_status VARCHAR(32) NOT NULL CHECK (onboarding_status IN ('INVITED', 'PENDING_DOCUMENTS', 'BACKGROUND_CHECK', 'APPROVED', 'REJECTED')),
    license_number VARCHAR(64) NOT NULL,
    license_country_code VARCHAR(3) NOT NULL,
    license_expires_at DATE NOT NULL,
    average_rating NUMERIC(3,2) NOT NULL DEFAULT 5.00,
    lifetime_trip_count INTEGER NOT NULL DEFAULT 0,
    acceptance_rate NUMERIC(5,2) NOT NULL DEFAULT 100.00,
    cancellation_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    risk_score NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    current_vehicle_id UUID,
    background_check_completed_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_driver_profile_user_profile UNIQUE (user_profile_id),
    CONSTRAINT uk_driver_profile_driver_code UNIQUE (driver_code)
);

CREATE UNIQUE INDEX uk_driver_profile_license_active
    ON driver.driver_profile (license_country_code, license_number)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_driver_profile_status
    ON driver.driver_profile (driver_status, onboarding_status)
    WHERE deleted_at IS NULL;

CREATE TABLE driver.vehicle (
    id UUID PRIMARY KEY,
    driver_profile_id UUID NOT NULL REFERENCES driver.driver_profile (id),
    vehicle_status VARCHAR(32) NOT NULL CHECK (vehicle_status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'RETIRED')),
    vehicle_type VARCHAR(32) NOT NULL CHECK (vehicle_type IN ('SEDAN', 'SUV', 'HATCHBACK', 'AUTO_RICKSHAW', 'BIKE', 'VAN', 'LUXURY')),
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    model_year SMALLINT NOT NULL CHECK (model_year >= 1990),
    color VARCHAR(50),
    registration_number VARCHAR(32) NOT NULL,
    vin VARCHAR(64),
    seat_capacity SMALLINT NOT NULL CHECK (seat_capacity BETWEEN 1 AND 12),
    luggage_capacity SMALLINT NOT NULL DEFAULT 0 CHECK (luggage_capacity BETWEEN 0 AND 12),
    wheelchair_accessible BOOLEAN NOT NULL DEFAULT FALSE,
    air_conditioned BOOLEAN NOT NULL DEFAULT TRUE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    insurance_expires_at DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_vehicle_registration_number_active
    ON driver.vehicle (LOWER(registration_number))
    WHERE deleted_at IS NULL;
CREATE INDEX idx_vehicle_driver_status
    ON driver.vehicle (driver_profile_id, vehicle_status, is_primary)
    WHERE deleted_at IS NULL;

ALTER TABLE driver.driver_profile
    ADD CONSTRAINT fk_driver_profile_current_vehicle
    FOREIGN KEY (current_vehicle_id) REFERENCES driver.vehicle (id);

CREATE TABLE driver.driver_availability (
    id UUID PRIMARY KEY,
    driver_profile_id UUID NOT NULL REFERENCES driver.driver_profile (id),
    availability_status VARCHAR(32) NOT NULL CHECK (availability_status IN ('AVAILABLE', 'BUSY', 'OFFLINE', 'BREAK', 'ON_TRIP')),
    online_status VARCHAR(32) NOT NULL CHECK (online_status IN ('ONLINE', 'OFFLINE', 'STALE')),
    current_session_id VARCHAR(128),
    available_seat_count SMALLINT NOT NULL DEFAULT 0 CHECK (available_seat_count BETWEEN 0 AND 12),
    current_ride_id UUID,
    last_location GEOGRAPHY(Point, 4326),
    last_location_accuracy_meters NUMERIC(8,2),
    last_heartbeat_at TIMESTAMPTZ,
    app_version VARCHAR(32),
    device_platform VARCHAR(32),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_driver_availability_driver UNIQUE (driver_profile_id)
);

CREATE INDEX idx_driver_availability_state
    ON driver.driver_availability (availability_status, online_status, last_heartbeat_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_driver_availability_last_location_gist
    ON driver.driver_availability
    USING GIST (last_location);

CREATE TABLE payment.payment_method (
    id UUID PRIMARY KEY,
    rider_profile_id UUID NOT NULL REFERENCES rider.rider_profile (id),
    payment_provider VARCHAR(32) NOT NULL CHECK (payment_provider IN ('STRIPE', 'RAZORPAY', 'ADYEN', 'BRAINTREE', 'INTERNAL')),
    payment_method_type VARCHAR(32) NOT NULL CHECK (payment_method_type IN ('CARD', 'UPI', 'WALLET', 'BANK_ACCOUNT', 'CASH')),
    payment_method_status VARCHAR(32) NOT NULL CHECK (payment_method_status IN ('ACTIVE', 'INACTIVE', 'EXPIRED', 'REVOKED')),
    provider_customer_ref VARCHAR(128),
    provider_payment_method_ref VARCHAR(128),
    card_brand VARCHAR(32),
    card_last4 VARCHAR(4),
    expiry_month SMALLINT CHECK (expiry_month BETWEEN 1 AND 12),
    expiry_year SMALLINT,
    billing_country_code VARCHAR(3),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE UNIQUE INDEX uk_payment_method_provider_ref_active
    ON payment.payment_method (payment_provider, provider_payment_method_ref)
    WHERE deleted_at IS NULL AND provider_payment_method_ref IS NOT NULL;
CREATE INDEX idx_payment_method_rider_default
    ON payment.payment_method (rider_profile_id, is_default, payment_method_status)
    WHERE deleted_at IS NULL;

CREATE TABLE pricing.fare_quote (
    id UUID PRIMARY KEY,
    pricing_status VARCHAR(32) NOT NULL CHECK (pricing_status IN ('ESTIMATED', 'LOCKED', 'FINALIZED', 'VOIDED')),
    currency_code CHAR(3) NOT NULL,
    base_fare NUMERIC(12,2) NOT NULL DEFAULT 0,
    distance_fare NUMERIC(12,2) NOT NULL DEFAULT 0,
    duration_fare NUMERIC(12,2) NOT NULL DEFAULT 0,
    surge_multiplier NUMERIC(8,4) NOT NULL DEFAULT 1.0000,
    surge_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    booking_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    toll_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    pooling_discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    rounding_adjustment NUMERIC(12,2) NOT NULL DEFAULT 0,
    subtotal_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    pricing_strategy_code VARCHAR(64),
    quoted_distance_meters INTEGER,
    quoted_duration_seconds INTEGER,
    expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_fare_quote_status_expires
    ON pricing.fare_quote (pricing_status, expires_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE ride.ride_request (
    id UUID PRIMARY KEY,
    rider_profile_id UUID NOT NULL REFERENCES rider.rider_profile (id),
    requested_ride_type VARCHAR(32) NOT NULL CHECK (requested_ride_type IN ('STANDARD', 'SHARED')),
    request_status VARCHAR(32) NOT NULL CHECK (request_status IN ('REQUESTED', 'SEARCHING_DRIVER', 'DRIVER_ASSIGNED', 'DRIVER_ARRIVING', 'DRIVER_ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED')),
    seat_count SMALLINT NOT NULL DEFAULT 1 CHECK (seat_count BETWEEN 1 AND 6),
    requested_vehicle_type VARCHAR(32) CHECK (requested_vehicle_type IN ('SEDAN', 'SUV', 'HATCHBACK', 'AUTO_RICKSHAW', 'BIKE', 'VAN', 'LUXURY')),
    fare_quote_id UUID REFERENCES pricing.fare_quote (id),
    payment_method_id UUID REFERENCES payment.payment_method (id),
    origin GEOGRAPHY(Point, 4326) NOT NULL,
    destination GEOGRAPHY(Point, 4326) NOT NULL,
    origin_address VARCHAR(500) NOT NULL,
    destination_address VARCHAR(500) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_for TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    matching_batch_key VARCHAR(64),
    notes VARCHAR(500),
    cancel_reason VARCHAR(255),
    cancelled_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_ride_request_rider_status
    ON ride.ride_request (rider_profile_id, request_status, requested_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_ride_request_matching
    ON ride.ride_request (request_status, requested_ride_type, scheduled_for, requested_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_ride_request_origin_gist
    ON ride.ride_request USING GIST (origin);
CREATE INDEX idx_ride_request_destination_gist
    ON ride.ride_request USING GIST (destination);

CREATE TABLE sharedride.shared_ride_group (
    id UUID PRIMARY KEY,
    group_status VARCHAR(32) NOT NULL CHECK (group_status IN ('FORMING', 'OPEN', 'LOCKED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    anchor_ride_request_id UUID REFERENCES ride.ride_request (id),
    max_seat_capacity SMALLINT NOT NULL CHECK (max_seat_capacity BETWEEN 2 AND 12),
    occupied_seat_count SMALLINT NOT NULL DEFAULT 0 CHECK (occupied_seat_count BETWEEN 0 AND 12),
    route_distance_meters INTEGER,
    route_duration_seconds INTEGER,
    detour_seconds INTEGER,
    pooling_savings_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    formed_at TIMESTAMPTZ,
    locked_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_shared_ride_group_status
    ON sharedride.shared_ride_group (group_status, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE ride.ride (
    id UUID PRIMARY KEY,
    public_ride_code VARCHAR(32) NOT NULL,
    booking_request_id UUID REFERENCES ride.ride_request (id),
    booking_rider_profile_id UUID REFERENCES rider.rider_profile (id),
    driver_profile_id UUID REFERENCES driver.driver_profile (id),
    vehicle_id UUID REFERENCES driver.vehicle (id),
    shared_ride_group_id UUID REFERENCES sharedride.shared_ride_group (id),
    ride_type VARCHAR(32) NOT NULL CHECK (ride_type IN ('STANDARD', 'SHARED')),
    lifecycle_status VARCHAR(32) NOT NULL CHECK (lifecycle_status IN ('REQUESTED', 'SEARCHING_DRIVER', 'DRIVER_ASSIGNED', 'DRIVER_ARRIVING', 'DRIVER_ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'FAILED')),
    current_stop_sequence INTEGER,
    assigned_at TIMESTAMPTZ,
    pickup_started_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_actor_type VARCHAR(32) CHECK (cancellation_actor_type IN ('RIDER', 'DRIVER', 'SYSTEM', 'ADMIN')),
    cancellation_reason VARCHAR(255),
    final_fare_quote_id UUID REFERENCES pricing.fare_quote (id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_ride_public_ride_code UNIQUE (public_ride_code)
);

CREATE INDEX idx_ride_driver_status
    ON ride.ride (driver_profile_id, lifecycle_status, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_ride_booking_rider_status
    ON ride.ride (booking_rider_profile_id, lifecycle_status, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_ride_shared_group
    ON ride.ride (shared_ride_group_id, lifecycle_status)
    WHERE deleted_at IS NULL;

CREATE TABLE ride.ride_stop (
    id UUID PRIMARY KEY,
    ride_request_id UUID NOT NULL REFERENCES ride.ride_request (id),
    ride_id UUID REFERENCES ride.ride (id),
    rider_profile_id UUID NOT NULL REFERENCES rider.rider_profile (id),
    stop_type VARCHAR(32) NOT NULL CHECK (stop_type IN ('PICKUP', 'DROPOFF', 'WAYPOINT')),
    stop_status VARCHAR(32) NOT NULL CHECK (stop_status IN ('PLANNED', 'SKIPPED', 'ARRIVED', 'COMPLETED', 'CANCELLED')),
    request_sequence_no INTEGER NOT NULL CHECK (request_sequence_no >= 1),
    ride_sequence_no INTEGER CHECK (ride_sequence_no >= 1),
    stop_point GEOGRAPHY(Point, 4326) NOT NULL,
    address_line VARCHAR(500) NOT NULL,
    locality VARCHAR(120),
    geohash VARCHAR(16),
    passenger_count SMALLINT NOT NULL DEFAULT 1 CHECK (passenger_count BETWEEN 1 AND 6),
    planned_arrival_at TIMESTAMPTZ,
    actual_arrival_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    detour_seconds INTEGER,
    notes VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_ride_stop_request_sequence
    ON ride.ride_stop (ride_request_id, request_sequence_no)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_ride_stop_ride_sequence
    ON ride.ride_stop (ride_id, ride_sequence_no)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_ride_stop_point_gist
    ON ride.ride_stop USING GIST (stop_point);

CREATE TABLE sharedride.shared_ride_candidate (
    id UUID PRIMARY KEY,
    base_ride_request_id UUID NOT NULL REFERENCES ride.ride_request (id),
    candidate_ride_request_id UUID NOT NULL REFERENCES ride.ride_request (id),
    proposed_group_id UUID REFERENCES sharedride.shared_ride_group (id),
    evaluation_status VARCHAR(32) NOT NULL CHECK (evaluation_status IN ('PENDING', 'COMPATIBLE', 'INCOMPATIBLE', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    compatibility_score NUMERIC(8,4) NOT NULL DEFAULT 0,
    overlap_distance_meters INTEGER,
    detour_delta_seconds INTEGER,
    estimated_savings_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    seat_fit BOOLEAN NOT NULL DEFAULT TRUE,
    rejection_reason VARCHAR(255),
    evaluation_metadata_json JSONB,
    evaluated_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_shared_ride_candidate_distinct_requests CHECK (base_ride_request_id <> candidate_ride_request_id)
);

CREATE UNIQUE INDEX uk_shared_ride_candidate_pair
    ON sharedride.shared_ride_candidate (base_ride_request_id, candidate_ride_request_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_shared_ride_candidate_status_score
    ON sharedride.shared_ride_candidate (evaluation_status, compatibility_score DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE tracking.driver_location_history (
    id UUID PRIMARY KEY,
    driver_profile_id UUID NOT NULL REFERENCES driver.driver_profile (id),
    ride_id UUID REFERENCES ride.ride (id),
    location GEOGRAPHY(Point, 4326) NOT NULL,
    heading_degrees NUMERIC(6,2),
    speed_kph NUMERIC(8,2),
    accuracy_meters NUMERIC(8,2),
    location_provider VARCHAR(32) CHECK (location_provider IN ('GPS', 'NETWORK', 'FUSED', 'MANUAL')),
    captured_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_driver_location_history_driver_captured_at
    ON tracking.driver_location_history (driver_profile_id, captured_at DESC);
CREATE INDEX idx_driver_location_history_ride_captured_at
    ON tracking.driver_location_history (ride_id, captured_at DESC);
CREATE INDEX idx_driver_location_history_location_gist
    ON tracking.driver_location_history USING GIST (location);

CREATE TABLE ride.ride_status_history (
    id UUID PRIMARY KEY,
    ride_request_id UUID REFERENCES ride.ride_request (id),
    ride_id UUID REFERENCES ride.ride (id),
    previous_status VARCHAR(32),
    current_status VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL CHECK (source_type IN ('RIDE_REQUEST', 'RIDE')),
    actor_type VARCHAR(32) CHECK (actor_type IN ('RIDER', 'DRIVER', 'SYSTEM', 'ADMIN')),
    actor_user_profile_id UUID REFERENCES identity.user_profile (id),
    reason_code VARCHAR(64),
    note VARCHAR(500),
    metadata_json JSONB,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_ride_status_history_target CHECK (
        (ride_request_id IS NOT NULL AND source_type = 'RIDE_REQUEST')
        OR
        (ride_id IS NOT NULL AND source_type = 'RIDE')
    )
);

CREATE INDEX idx_ride_status_history_request_changed_at
    ON ride.ride_status_history (ride_request_id, changed_at DESC);
CREATE INDEX idx_ride_status_history_ride_changed_at
    ON ride.ride_status_history (ride_id, changed_at DESC);

CREATE TABLE pricing.fare_breakdown_item (
    id UUID PRIMARY KEY,
    fare_quote_id UUID NOT NULL REFERENCES pricing.fare_quote (id),
    line_type VARCHAR(32) NOT NULL CHECK (line_type IN ('BASE', 'DISTANCE', 'DURATION', 'SURGE', 'BOOKING_FEE', 'TAX', 'TOLL', 'DISCOUNT', 'POOLING_DISCOUNT', 'ROUNDING')),
    line_code VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    amount NUMERIC(12,2) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fare_breakdown_item_quote_order
    ON pricing.fare_breakdown_item (fare_quote_id, sort_order);

CREATE TABLE payment.payment_transaction (
    id UUID PRIMARY KEY,
    ride_request_id UUID REFERENCES ride.ride_request (id),
    ride_id UUID REFERENCES ride.ride (id),
    payment_method_id UUID REFERENCES payment.payment_method (id),
    payment_provider VARCHAR(32) NOT NULL CHECK (payment_provider IN ('STRIPE', 'RAZORPAY', 'ADYEN', 'BRAINTREE', 'INTERNAL')),
    transaction_type VARCHAR(32) NOT NULL CHECK (transaction_type IN ('AUTHORIZATION', 'CAPTURE', 'SALE', 'REFUND', 'VOID')),
    transaction_status VARCHAR(32) NOT NULL CHECK (transaction_status IN ('PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'VOIDED')),
    provider_transaction_ref VARCHAR(128),
    provider_idempotency_key VARCHAR(128),
    amount NUMERIC(12,2) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    failure_reason VARCHAR(255),
    authorized_at TIMESTAMPTZ,
    captured_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    refunded_at TIMESTAMPTZ,
    metadata_json JSONB,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_payment_transaction_ride_status
    ON payment.payment_transaction (ride_id, transaction_status, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_payment_transaction_provider_ref_active
    ON payment.payment_transaction (payment_provider, provider_transaction_ref)
    WHERE deleted_at IS NULL AND provider_transaction_ref IS NOT NULL;

CREATE TABLE fraud.fraud_flag (
    id UUID PRIMARY KEY,
    subject_type VARCHAR(32) NOT NULL CHECK (subject_type IN ('USER', 'RIDER', 'DRIVER', 'RIDE_REQUEST', 'RIDE', 'PAYMENT')),
    subject_id UUID NOT NULL,
    severity VARCHAR(32) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    flag_status VARCHAR(32) NOT NULL CHECK (flag_status IN ('OPEN', 'UNDER_REVIEW', 'CONFIRMED', 'DISMISSED', 'RESOLVED')),
    rule_code VARCHAR(64) NOT NULL,
    risk_score NUMERIC(8,4),
    title VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    evidence_json JSONB,
    resolved_by_user_profile_id UUID REFERENCES identity.user_profile (id),
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_fraud_flag_subject_status
    ON fraud.fraud_flag (subject_type, subject_id, flag_status, severity)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_fraud_flag_open_created_at
    ON fraud.fraud_flag (flag_status, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE rating.rating_review (
    id UUID PRIMARY KEY,
    ride_id UUID NOT NULL REFERENCES ride.ride (id),
    reviewer_user_profile_id UUID NOT NULL REFERENCES identity.user_profile (id),
    reviewed_user_profile_id UUID NOT NULL REFERENCES identity.user_profile (id),
    reviewer_type VARCHAR(32) NOT NULL CHECK (reviewer_type IN ('RIDER', 'DRIVER')),
    reviewed_type VARCHAR(32) NOT NULL CHECK (reviewed_type IN ('RIDER', 'DRIVER')),
    rating_value SMALLINT NOT NULL CHECK (rating_value BETWEEN 1 AND 5),
    review_text VARCHAR(1000),
    tags_json JSONB,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_rating_review_distinct_users CHECK (reviewer_user_profile_id <> reviewed_user_profile_id)
);

CREATE UNIQUE INDEX uk_rating_review_unique_direction
    ON rating.rating_review (ride_id, reviewer_user_profile_id, reviewed_user_profile_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_rating_review_reviewed_user
    ON rating.rating_review (reviewed_user_profile_id, submitted_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE notification.notification (
    id UUID PRIMARY KEY,
    user_profile_id UUID NOT NULL REFERENCES identity.user_profile (id),
    ride_id UUID REFERENCES ride.ride (id),
    notification_type VARCHAR(32) NOT NULL CHECK (notification_type IN ('RIDE_UPDATE', 'PAYMENT', 'SECURITY', 'PROMOTION', 'ADMIN_ALERT')),
    channel VARCHAR(32) NOT NULL CHECK (channel IN ('PUSH', 'SMS', 'EMAIL', 'IN_APP', 'WEBHOOK')),
    delivery_status VARCHAR(32) NOT NULL CHECK (delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'CANCELLED')),
    template_key VARCHAR(100),
    title VARCHAR(200),
    body VARCHAR(2000),
    payload_json JSONB,
    provider_message_ref VARCHAR(128),
    scheduled_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    failure_reason VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_notification_user_status
    ON notification.notification (user_profile_id, delivery_status, created_at DESC)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_notification_ride
    ON notification.notification (ride_id, notification_type, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE TABLE admin.admin_audit_log (
    id UUID PRIMARY KEY,
    actor_user_profile_id UUID REFERENCES identity.user_profile (id),
    action_code VARCHAR(100) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id UUID,
    result_status VARCHAR(32) NOT NULL CHECK (result_status IN ('SUCCESS', 'FAILED', 'DENIED')),
    request_id VARCHAR(128),
    trace_id VARCHAR(128),
    source_ip VARCHAR(64),
    user_agent VARCHAR(500),
    metadata_json JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_log_actor_occurred_at
    ON admin.admin_audit_log (actor_user_profile_id, occurred_at DESC);
CREATE INDEX idx_admin_audit_log_target
    ON admin.admin_audit_log (target_type, target_id, occurred_at DESC);
