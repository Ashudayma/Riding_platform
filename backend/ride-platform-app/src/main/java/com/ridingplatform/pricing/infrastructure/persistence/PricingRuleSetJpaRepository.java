package com.ridingplatform.pricing.infrastructure.persistence;

import com.ridingplatform.driver.infrastructure.persistence.VehicleType;
import com.ridingplatform.ride.domain.RideType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PricingRuleSetJpaRepository extends JpaRepository<PricingRuleSetEntity, UUID> {

    @Query("""
            select rule
            from PricingRuleSetEntity rule
            where rule.deletedAt is null
              and (:cityCode is null or rule.cityCode = :cityCode)
              and (:active is null or rule.active = :active)
            """)
    Page<PricingRuleSetEntity> searchAdmin(
            @Param("cityCode") String cityCode,
            @Param("active") Boolean active,
            Pageable pageable
    );

    @Query("""
            select rule
            from PricingRuleSetEntity rule
            where rule.active = true
              and rule.deletedAt is null
              and rule.cityCode = :cityCode
              and (:zoneCode is null or rule.zoneCode = :zoneCode or rule.zoneCode is null)
              and rule.rideType = :rideType
              and (:vehicleType is null or rule.vehicleType = :vehicleType or rule.vehicleType is null)
              and rule.effectiveFrom <= :instant
              and (rule.effectiveTo is null or rule.effectiveTo > :instant)
            order by
              case when rule.zoneCode = :zoneCode then 0 else 1 end,
              case when rule.vehicleType = :vehicleType then 0 else 1 end,
              rule.pricingVersion desc,
              rule.effectiveFrom desc
            """)
    List<PricingRuleSetEntity> findApplicableRules(
            @Param("cityCode") String cityCode,
            @Param("zoneCode") String zoneCode,
            @Param("rideType") RideType rideType,
            @Param("vehicleType") VehicleType vehicleType,
            @Param("instant") Instant instant
    );
}
