INSERT INTO notification.notification_template (id, event_code, channel, locale, title_template, body_template)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'RIDE_BOOKED', 'IN_APP', 'en', 'Ride booked', 'Your ride {{rideCode}} has been booked. We are finding a driver.'),
    ('b1000000-0000-0000-0000-000000000002', 'RIDE_BOOKED', 'PUSH', 'en', 'Ride booked', 'Your ride {{rideCode}} is booked. Driver search has started.'),
    ('b1000000-0000-0000-0000-000000000003', 'DRIVER_ASSIGNED_RIDER', 'IN_APP', 'en', 'Driver assigned', '{{driverName}} has been assigned to ride {{rideCode}}.'),
    ('b1000000-0000-0000-0000-000000000004', 'DRIVER_ASSIGNED_RIDER', 'PUSH', 'en', 'Driver assigned', '{{driverName}} is on the way for ride {{rideCode}}.'),
    ('b1000000-0000-0000-0000-000000000005', 'DRIVER_ASSIGNED_DRIVER', 'IN_APP', 'en', 'New ride assignment', 'You have been assigned ride {{rideCode}}.'),
    ('b1000000-0000-0000-0000-000000000006', 'DRIVER_ASSIGNED_DRIVER', 'PUSH', 'en', 'New ride assignment', 'Pickup assigned for ride {{rideCode}}.'),
    ('b1000000-0000-0000-0000-000000000007', 'RIDE_CANCELLED', 'IN_APP', 'en', 'Ride cancelled', 'Ride {{rideCode}} has been cancelled.'),
    ('b1000000-0000-0000-0000-000000000008', 'PAYMENT_FAILED', 'EMAIL', 'en', 'Payment failed', 'Payment for ride {{rideCode}} could not be completed. Please update your payment method.'),
    ('b1000000-0000-0000-0000-000000000009', 'PAYMENT_CAPTURED', 'EMAIL', 'en', 'Payment received', 'Payment for ride {{rideCode}} has been captured successfully.'),
    ('b1000000-0000-0000-0000-000000000010', 'ACCOUNT_BLOCKED', 'IN_APP', 'en', 'Account action required', 'Your account has been temporarily restricted. Please contact support.'),
    ('b1000000-0000-0000-0000-000000000011', 'ACCOUNT_BLOCKED', 'EMAIL', 'en', 'Account restricted', 'Your account has been restricted due to a security or fraud review.'),
    ('b1000000-0000-0000-0000-000000000012', 'FRAUD_ALERT_ANALYST', 'IN_APP', 'en', 'Fraud alert raised', 'A new fraud alert has been raised for review.')
ON CONFLICT DO NOTHING;
