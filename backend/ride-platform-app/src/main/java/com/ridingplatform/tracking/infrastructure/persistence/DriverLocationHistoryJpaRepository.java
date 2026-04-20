package com.ridingplatform.tracking.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverLocationHistoryJpaRepository extends JpaRepository<DriverLocationHistoryEntity, UUID> {

    List<DriverLocationHistoryEntity> findTop100ByDriverProfileIdOrderByCapturedAtDesc(UUID driverProfileId);
}
