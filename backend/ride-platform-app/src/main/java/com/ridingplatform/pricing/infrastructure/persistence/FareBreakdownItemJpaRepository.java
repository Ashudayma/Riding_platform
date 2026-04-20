package com.ridingplatform.pricing.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FareBreakdownItemJpaRepository extends JpaRepository<FareBreakdownItemEntity, UUID> {

    List<FareBreakdownItemEntity> findByFareQuoteIdOrderBySortOrderAsc(UUID fareQuoteId);
}
