INSERT INTO identity.user_profile (
    id, keycloak_user_id, user_status, email, phone_country_code, phone_number,
    first_name, last_name, display_name, country_code, timezone, default_locale,
    email_verified, phone_verified, metadata_json
) VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        '20000000-0000-0000-0000-000000000001',
        'ACTIVE',
        'rider1@example.com',
        '+91',
        '9999990001',
        'Riya',
        'Sharma',
        'Riya Sharma',
        'IN',
        'Asia/Kolkata',
        'en-IN',
        TRUE,
        TRUE,
        '{"seed":"local-dev","persona":"rider"}'
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        '20000000-0000-0000-0000-000000000002',
        'ACTIVE',
        'driver1@example.com',
        '+91',
        '9999990002',
        'Arjun',
        'Verma',
        'Arjun Verma',
        'IN',
        'Asia/Kolkata',
        'en-IN',
        TRUE,
        TRUE,
        '{"seed":"local-dev","persona":"driver"}'
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        '20000000-0000-0000-0000-000000000003',
        'ACTIVE',
        'admin1@example.com',
        '+91',
        '9999990003',
        'Nisha',
        'Kapoor',
        'Nisha Kapoor',
        'IN',
        'Asia/Kolkata',
        'en-IN',
        TRUE,
        TRUE,
        '{"seed":"local-dev","persona":"admin"}'
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        '20000000-0000-0000-0000-000000000004',
        'ACTIVE',
        'rider2@example.com',
        '+91',
        '9999990004',
        'Kabir',
        'Singh',
        'Kabir Singh',
        'IN',
        'Asia/Kolkata',
        'en-IN',
        TRUE,
        TRUE,
        '{"seed":"local-dev","persona":"rider"}'
    );

INSERT INTO rider.rider_profile (
    id, user_profile_id, rider_code, rider_status, average_rating, lifetime_ride_count,
    cancellation_count, no_show_count, fraud_hold, preferred_language,
    emergency_contact_name, emergency_contact_phone
) VALUES (
    '30000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'RIDER-0001',
    'ACTIVE',
    4.90,
    128,
    3,
    1,
    FALSE,
    'en',
    'Aman Sharma',
    '9999991001'
);

INSERT INTO rider.rider_profile (
    id, user_profile_id, rider_code, rider_status, average_rating, lifetime_ride_count,
    cancellation_count, no_show_count, fraud_hold, preferred_language
) VALUES (
    '30000000-0000-0000-0000-000000000002',
    '10000000-0000-0000-0000-000000000004',
    'RIDER-0002',
    'ACTIVE',
    4.70,
    45,
    1,
    0,
    FALSE,
    'en'
);

INSERT INTO driver.driver_profile (
    id, user_profile_id, driver_code, driver_status, onboarding_status, license_number,
    license_country_code, license_expires_at, average_rating, lifetime_trip_count,
    acceptance_rate, cancellation_rate, risk_score, background_check_completed_at, approved_at
) VALUES (
    '40000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    'DRIVER-0001',
    'ACTIVE',
    'APPROVED',
    'DL-11223344',
    'IN',
    DATE '2029-12-31',
    4.95,
    2100,
    92.50,
    2.10,
    0.80,
    CURRENT_TIMESTAMP - INTERVAL '45 days',
    CURRENT_TIMESTAMP - INTERVAL '40 days'
);

INSERT INTO driver.vehicle (
    id, driver_profile_id, vehicle_status, vehicle_type, make, model, model_year, color,
    registration_number, seat_capacity, luggage_capacity, wheelchair_accessible,
    air_conditioned, is_primary, verified_at, insurance_expires_at
) VALUES (
    '50000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    'ACTIVE',
    'SEDAN',
    'Hyundai',
    'Verna',
    2023,
    'White',
    'DL01AB1234',
    4,
    2,
    FALSE,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP - INTERVAL '30 days',
    DATE '2027-03-31'
);

UPDATE driver.driver_profile
SET current_vehicle_id = '50000000-0000-0000-0000-000000000001'
WHERE id = '40000000-0000-0000-0000-000000000001';

INSERT INTO driver.driver_availability (
    id, driver_profile_id, availability_status, online_status, current_session_id,
    available_seat_count, last_location, last_location_accuracy_meters, last_heartbeat_at,
    app_version, device_platform
) VALUES (
    '60000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    'AVAILABLE',
    'ONLINE',
    'session-driver-0001',
    4,
    ST_GeogFromText('SRID=4326;POINT(77.2090 28.6139)'),
    6.5,
    CURRENT_TIMESTAMP,
    '1.0.0',
    'ANDROID'
);

INSERT INTO payment.payment_method (
    id, rider_profile_id, payment_provider, payment_method_type, payment_method_status,
    provider_customer_ref, provider_payment_method_ref, card_brand, card_last4,
    expiry_month, expiry_year, billing_country_code, is_default
) VALUES (
    '70000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    'STRIPE',
    'CARD',
    'ACTIVE',
    'cus_local_rider_01',
    'pm_local_card_01',
    'VISA',
    '4242',
    12,
    2028,
    'IN',
    TRUE
);

INSERT INTO pricing.fare_quote (
    id, pricing_status, currency_code, base_fare, distance_fare, duration_fare,
    surge_multiplier, surge_amount, booking_fee, tax_amount, discount_amount,
    pooling_discount_amount, subtotal_amount, total_amount, pricing_strategy_code,
    quoted_distance_meters, quoted_duration_seconds, expires_at
) VALUES (
    '80000000-0000-0000-0000-000000000001',
    'ESTIMATED',
    'INR',
    60.00,
    110.00,
    40.00,
    1.1500,
    24.15,
    15.00,
    22.35,
    0.00,
    18.00,
    231.50,
    213.50,
    'STANDARD_V1',
    14300,
    2100,
    CURRENT_TIMESTAMP + INTERVAL '10 minutes'
);

INSERT INTO pricing.fare_breakdown_item (
    id, fare_quote_id, line_type, line_code, description, amount, sort_order
) VALUES
    ('81000000-0000-0000-0000-000000000001', '80000000-0000-0000-0000-000000000001', 'BASE', 'BASE_FARE', 'Base fare', 60.00, 1),
    ('81000000-0000-0000-0000-000000000002', '80000000-0000-0000-0000-000000000001', 'DISTANCE', 'DISTANCE_FARE', 'Distance charge', 110.00, 2),
    ('81000000-0000-0000-0000-000000000003', '80000000-0000-0000-0000-000000000001', 'DURATION', 'TIME_FARE', 'Duration charge', 40.00, 3),
    ('81000000-0000-0000-0000-000000000004', '80000000-0000-0000-0000-000000000001', 'SURGE', 'SURGE', 'Demand surge', 24.15, 4),
    ('81000000-0000-0000-0000-000000000005', '80000000-0000-0000-0000-000000000001', 'BOOKING_FEE', 'BOOKING_FEE', 'Booking fee', 15.00, 5),
    ('81000000-0000-0000-0000-000000000006', '80000000-0000-0000-0000-000000000001', 'POOLING_DISCOUNT', 'POOL_DISCOUNT', 'Shared ride savings', -18.00, 6),
    ('81000000-0000-0000-0000-000000000007', '80000000-0000-0000-0000-000000000001', 'TAX', 'GST', 'Tax', 22.35, 7);

INSERT INTO ride.ride_request (
    id, rider_profile_id, requested_ride_type, request_status, seat_count, requested_vehicle_type,
    fare_quote_id, payment_method_id, origin, destination, origin_address, destination_address,
    requested_at, scheduled_for, expires_at, matching_batch_key, notes
) VALUES (
    '90000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    'SHARED',
    'SEARCHING_DRIVER',
    1,
    'SEDAN',
    '80000000-0000-0000-0000-000000000001',
    '70000000-0000-0000-0000-000000000001',
    ST_GeogFromText('SRID=4326;POINT(77.2090 28.6139)'),
    ST_GeogFromText('SRID=4326;POINT(77.0266 28.4595)'),
    'Connaught Place, New Delhi',
    'Cyber Hub, Gurugram',
    CURRENT_TIMESTAMP - INTERVAL '2 minutes',
    NULL,
    CURRENT_TIMESTAMP + INTERVAL '8 minutes',
    'MATCH-BATCH-0001',
    'Need a quiet ride'
);

INSERT INTO ride.ride_stop (
    id, ride_request_id, rider_profile_id, stop_type, stop_status, request_sequence_no,
    stop_point, address_line, locality, geohash, passenger_count
) VALUES
    (
        '91000000-0000-0000-0000-000000000001',
        '90000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000001',
        'PICKUP',
        'PLANNED',
        1,
        ST_GeogFromText('SRID=4326;POINT(77.2090 28.6139)'),
        'Connaught Place, New Delhi',
        'New Delhi',
        'ttnfv2u',
        1
    ),
    (
        '91000000-0000-0000-0000-000000000002',
        '90000000-0000-0000-0000-000000000001',
        '30000000-0000-0000-0000-000000000001',
        'DROPOFF',
        'PLANNED',
        2,
        ST_GeogFromText('SRID=4326;POINT(77.0266 28.4595)'),
        'Cyber Hub, Gurugram',
        'Gurugram',
        'ttn4ptw',
        1
    );

INSERT INTO sharedride.shared_ride_group (
    id, group_status, anchor_ride_request_id, max_seat_capacity, occupied_seat_count,
    route_distance_meters, route_duration_seconds, detour_seconds, pooling_savings_amount, formed_at
) VALUES (
    '92000000-0000-0000-0000-000000000001',
    'FORMING',
    '90000000-0000-0000-0000-000000000001',
    4,
    1,
    14300,
    2100,
    0,
    18.00,
    CURRENT_TIMESTAMP - INTERVAL '1 minute'
);

INSERT INTO ride.ride (
    id, public_ride_code, booking_request_id, booking_rider_profile_id, driver_profile_id,
    vehicle_id, shared_ride_group_id, ride_type, lifecycle_status, assigned_at
) VALUES (
    '93000000-0000-0000-0000-000000000001',
    'RIDE-20260417-0001',
    '90000000-0000-0000-0000-000000000001',
    '30000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '50000000-0000-0000-0000-000000000001',
    '92000000-0000-0000-0000-000000000001',
    'SHARED',
    'DRIVER_ASSIGNED',
    CURRENT_TIMESTAMP - INTERVAL '30 seconds'
);

UPDATE ride.ride_stop
SET ride_id = '93000000-0000-0000-0000-000000000001',
    ride_sequence_no = CASE
        WHEN stop_type = 'PICKUP' THEN 1
        ELSE 2
    END
WHERE ride_request_id = '90000000-0000-0000-0000-000000000001';

UPDATE driver.driver_availability
SET availability_status = 'ON_TRIP',
    current_ride_id = '93000000-0000-0000-0000-000000000001',
    available_seat_count = 3
WHERE id = '60000000-0000-0000-0000-000000000001';

INSERT INTO ride.ride_request (
    id, rider_profile_id, requested_ride_type, request_status, seat_count, requested_vehicle_type,
    origin, destination, origin_address, destination_address, requested_at, expires_at, matching_batch_key
) VALUES (
    '90000000-0000-0000-0000-000000000002',
    '30000000-0000-0000-0000-000000000002',
    'SHARED',
    'SEARCHING_DRIVER',
    1,
    'SEDAN',
    ST_GeogFromText('SRID=4326;POINT(77.1810 28.5920)'),
    ST_GeogFromText('SRID=4326;POINT(77.0560 28.4710)'),
    'India Gate, New Delhi',
    'MG Road, Gurugram',
    CURRENT_TIMESTAMP - INTERVAL '90 seconds',
    CURRENT_TIMESTAMP + INTERVAL '8 minutes',
    'MATCH-BATCH-0001'
);

INSERT INTO sharedride.shared_ride_candidate (
    id, base_ride_request_id, candidate_ride_request_id, proposed_group_id, evaluation_status,
    compatibility_score, overlap_distance_meters, detour_delta_seconds, estimated_savings_amount,
    seat_fit, evaluation_metadata_json, evaluated_at, expires_at
) VALUES (
    '94000000-0000-0000-0000-000000000001',
    '90000000-0000-0000-0000-000000000001',
    '90000000-0000-0000-0000-000000000002',
    '92000000-0000-0000-0000-000000000001',
    'COMPATIBLE',
    0.8825,
    9800,
    240,
    34.50,
    TRUE,
    '{"reason":"strong corridor overlap","seatCapacityAfterJoin":2}',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '5 minutes'
);

INSERT INTO tracking.driver_location_history (
    id, driver_profile_id, ride_id, location, heading_degrees, speed_kph, accuracy_meters, location_provider, captured_at
) VALUES (
    '95000000-0000-0000-0000-000000000001',
    '40000000-0000-0000-0000-000000000001',
    '93000000-0000-0000-0000-000000000001',
    ST_GeogFromText('SRID=4326;POINT(77.1950 28.6010)'),
    185.50,
    32.40,
    5.20,
    'GPS',
    CURRENT_TIMESTAMP - INTERVAL '15 seconds'
);

INSERT INTO ride.ride_status_history (
    id, ride_request_id, previous_status, current_status, source_type, actor_type, actor_user_profile_id, note
) VALUES (
    '96000000-0000-0000-0000-000000000001',
    '90000000-0000-0000-0000-000000000001',
    'REQUESTED',
    'SEARCHING_DRIVER',
    'RIDE_REQUEST',
    'SYSTEM',
    NULL,
    'Dispatch search started'
);

INSERT INTO ride.ride_status_history (
    id, ride_id, previous_status, current_status, source_type, actor_type, actor_user_profile_id, note
) VALUES (
    '96000000-0000-0000-0000-000000000002',
    '93000000-0000-0000-0000-000000000001',
    'REQUESTED',
    'DRIVER_ASSIGNED',
    'RIDE',
    'SYSTEM',
    NULL,
    'Driver assigned by dispatch'
);

INSERT INTO payment.payment_transaction (
    id, ride_request_id, ride_id, payment_method_id, payment_provider, transaction_type,
    transaction_status, provider_transaction_ref, provider_idempotency_key, amount, currency_code,
    authorized_at, metadata_json
) VALUES (
    '97000000-0000-0000-0000-000000000001',
    '90000000-0000-0000-0000-000000000001',
    '93000000-0000-0000-0000-000000000001',
    '70000000-0000-0000-0000-000000000001',
    'STRIPE',
    'AUTHORIZATION',
    'AUTHORIZED',
    'pi_local_0001',
    'idem_local_0001',
    213.50,
    'INR',
    CURRENT_TIMESTAMP - INTERVAL '20 seconds',
    '{"seed":"auth-only"}'
);

INSERT INTO fraud.fraud_flag (
    id, subject_type, subject_id, severity, flag_status, rule_code, risk_score, title, description, evidence_json
) VALUES (
    '98000000-0000-0000-0000-000000000001',
    'RIDE_REQUEST',
    '90000000-0000-0000-0000-000000000001',
    'LOW',
    'OPEN',
    'LATE_NIGHT_SHARED_PATTERN',
    0.2100,
    'Shared ride late-night pattern',
    'Low-severity heuristic trigger for monitoring only.',
    '{"ruleVersion":"1.0","features":{"lateNight":true,"sharedRide":true}}'
);

INSERT INTO rating.rating_review (
    id, ride_id, reviewer_user_profile_id, reviewed_user_profile_id, reviewer_type, reviewed_type,
    rating_value, review_text, tags_json
) VALUES (
    '99000000-0000-0000-0000-000000000001',
    '93000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    'RIDER',
    'DRIVER',
    5,
    'Polite and professional driver.',
    '["clean-car","on-time"]'
);

INSERT INTO notification.notification (
    id, user_profile_id, ride_id, notification_type, channel, delivery_status,
    template_key, title, body, payload_json, sent_at, delivered_at
) VALUES (
    'aa000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    '93000000-0000-0000-0000-000000000001',
    'RIDE_UPDATE',
    'PUSH',
    'DELIVERED',
    'ride.driver_assigned',
    'Driver assigned',
    'Arjun is on the way in a white Hyundai Verna.',
    '{"rideCode":"RIDE-20260417-0001"}',
    CURRENT_TIMESTAMP - INTERVAL '25 seconds',
    CURRENT_TIMESTAMP - INTERVAL '24 seconds'
);

INSERT INTO admin.admin_audit_log (
    id, actor_user_profile_id, action_code, target_type, target_id, result_status,
    request_id, trace_id, source_ip, user_agent, metadata_json
) VALUES (
    'ab000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000003',
    'FRAUD_FLAG_VIEW',
    'FRAUD_FLAG',
    '98000000-0000-0000-0000-000000000001',
    'SUCCESS',
    'req-local-0001',
    'trace-local-0001',
    '127.0.0.1',
    'local-seed-script',
    '{"seed":"local-dev"}'
);
