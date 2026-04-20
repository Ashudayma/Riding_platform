package com.ridingplatform.payment.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionJpaRepository extends JpaRepository<PaymentTransactionEntity, UUID> {

    List<PaymentTransactionEntity> findByRideIdOrderByCreatedAtDesc(UUID rideId);
}
