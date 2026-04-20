package com.ridingplatform.driver.application;

import com.ridingplatform.driver.infrastructure.persistence.DriverProfileEntity;
import com.ridingplatform.driver.infrastructure.persistence.DriverProfileJpaRepository;
import com.ridingplatform.security.application.SecurityContextFacade;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class DriverProfileResolver {

    private final SecurityContextFacade securityContextFacade;
    private final DriverProfileJpaRepository driverProfileJpaRepository;

    public DriverProfileResolver(
            SecurityContextFacade securityContextFacade,
            DriverProfileJpaRepository driverProfileJpaRepository
    ) {
        this.securityContextFacade = securityContextFacade;
        this.driverProfileJpaRepository = driverProfileJpaRepository;
    }

    public DriverProfileEntity currentDriverProfile() {
        UUID userProfileId = securityContextFacade.currentActor()
                .orElseThrow(() -> new DriverStateException("Authenticated driver not found"))
                .userProfileId();
        if (userProfileId == null) {
            throw new DriverStateException("user_profile_id claim is required");
        }
        return driverProfileJpaRepository.findByUserProfileId(userProfileId)
                .orElseThrow(() -> new DriverStateException("Driver profile not found for authenticated user"));
    }
}
