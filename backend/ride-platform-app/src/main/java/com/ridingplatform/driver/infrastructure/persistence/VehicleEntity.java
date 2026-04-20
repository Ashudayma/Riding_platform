package com.ridingplatform.driver.infrastructure.persistence;

import com.ridingplatform.common.persistence.AbstractJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle", schema = "driver")
public class VehicleEntity extends AbstractJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_profile_id", nullable = false)
    private DriverProfileEntity driverProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_status", nullable = false, length = 32)
    private VehicleStatus vehicleStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 32)
    private VehicleType vehicleType;

    @Column(nullable = false, length = 100)
    private String make;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "model_year", nullable = false)
    private short modelYear;

    @Column(length = 50)
    private String color;

    @Column(name = "registration_number", nullable = false, length = 32)
    private String registrationNumber;

    @Column(length = 64)
    private String vin;

    @Column(name = "seat_capacity", nullable = false)
    private short seatCapacity;

    @Column(name = "luggage_capacity", nullable = false)
    private short luggageCapacity;

    @Column(name = "wheelchair_accessible", nullable = false)
    private boolean wheelchairAccessible;

    @Column(name = "air_conditioned", nullable = false)
    private boolean airConditioned;

    @Column(name = "is_primary", nullable = false)
    private boolean primaryVehicle;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "insurance_expires_at")
    private LocalDate insuranceExpiresAt;
}
