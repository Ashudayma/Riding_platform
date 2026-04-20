package com.ridingplatform.ride.application;

import com.ridingplatform.config.ApplicationProperties;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptJpaRepository;
import com.ridingplatform.ride.infrastructure.persistence.DriverAssignmentAttemptStatus;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DriverAssignmentTimeoutScheduler {

    private final ApplicationProperties applicationProperties;
    private final DriverAssignmentAttemptJpaRepository driverAssignmentAttemptJpaRepository;
    private final DriverAssignmentService driverAssignmentService;
    private final Clock clock;

    public DriverAssignmentTimeoutScheduler(
            ApplicationProperties applicationProperties,
            DriverAssignmentAttemptJpaRepository driverAssignmentAttemptJpaRepository,
            DriverAssignmentService driverAssignmentService,
            Clock clock
    ) {
        this.applicationProperties = applicationProperties;
        this.driverAssignmentAttemptJpaRepository = driverAssignmentAttemptJpaRepository;
        this.driverAssignmentService = driverAssignmentService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "5000")
    public void expireTimedOutAssignments() {
        driverAssignmentAttemptJpaRepository.findTopExpiredByStatus(
                        DriverAssignmentAttemptStatus.PENDING_DRIVER_RESPONSE,
                        Instant.now(clock),
                        PageRequest.of(0, applicationProperties.dispatch().timeoutScanBatchSize())
                ).forEach(attempt -> driverAssignmentService.expirePendingAssignment(attempt.getId())
                        .ifPresent(rideRequestId -> driverAssignmentService.assignBestDriver(new DriverAssignmentRequest(
                                rideRequestId,
                                AssignmentTrigger.DRIVER_RESPONSE_TIMEOUT
                        ))));
    }
}
