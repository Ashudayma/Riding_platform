package com.ridingplatform.pricing.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FareQuoteJpaRepository extends JpaRepository<FareQuoteEntity, UUID> {
}
