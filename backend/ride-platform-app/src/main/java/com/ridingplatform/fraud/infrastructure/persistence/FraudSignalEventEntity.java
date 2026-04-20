package com.ridingplatform.fraud.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import com.ridingplatform.payment.infrastructure.persistence.PaymentTransactionEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideEntity;
import com.ridingplatform.ride.infrastructure.persistence.RideRequestEntity;
import com.ridingplatform.rider.infrastructure.persistence.RiderProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fraud_signal_event", schema = "fraud")
public class FraudSignalEventEntity extends AbstractJpaEntity {

    @Column(name = "signal_type", nullable = false, length = 64)
    private String signalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 32)
    private FraudSubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private java.util.UUID subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_profile_id")
    private RiderProfileEntity riderProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_profile_id")
    private DriverProfileEntity driverProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_request_id")
    private RideRequestEntity rideRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private RideEntity ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_transaction_id")
    private PaymentTransactionEntity paymentTransaction;

    @Column(name = "source_topic", length = 160)
    private String sourceTopic;

    @Column(name = "event_key", length = 128)
    private String eventKey;

    @Column(name = "attributes_json", columnDefinition = "jsonb")
    private String attributesJson;

    @Column(name = "triggered_rules_json", columnDefinition = "jsonb")
    private String triggeredRulesJson;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
