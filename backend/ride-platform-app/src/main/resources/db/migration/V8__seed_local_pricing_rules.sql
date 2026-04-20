INSERT INTO pricing.pricing_rule_set (
    id, city_code, zone_code, ride_type, vehicle_type, pricing_version, active, currency_code,
    base_fare, minimum_fare, booking_fee, per_km_rate, per_minute_rate, per_stop_charge,
    waiting_charge_per_minute, cancellation_base_charge, cancellation_per_km_charge,
    shared_discount_factor, tax_percentage, surge_cap_multiplier, night_surcharge_percentage,
    airport_surcharge_amount, effective_from, created_by, metadata_json, version, created_at, updated_at
) VALUES
(
    'a1000000-0000-0000-0000-000000000001', 'DELHI_NCR', 'DEFAULT', 'STANDARD', NULL, 1, TRUE, 'INR',
    70.00, 90.00, 15.00, 12.0000, 0.9000, 10.00,
    1.5000, 40.00, 4.0000,
    0.0000, 0.0500, 2.5000, 0.0000,
    0.00, CURRENT_TIMESTAMP - INTERVAL '1 day', 'seed', '{"name":"default-standard-v1"}', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
),
(
    'a1000000-0000-0000-0000-000000000002', 'DELHI_NCR', 'DEFAULT', 'SHARED', NULL, 1, TRUE, 'INR',
    60.00, 80.00, 12.00, 10.0000, 0.7000, 8.00,
    1.2000, 30.00, 3.0000,
    0.1800, 0.0500, 1.8000, 0.0000,
    0.00, CURRENT_TIMESTAMP - INTERVAL '1 day', 'seed', '{"name":"default-shared-v1"}', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
