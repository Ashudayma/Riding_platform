package com.ridingplatform.fraud.interfaces;

import com.ridingplatform.fraud.infrastructure.persistence.FraudFlagStatus;
import jakarta.validation.constraints.NotNull;

public record FraudFlagReviewHttpRequest(
        @NotNull FraudFlagStatus flagStatus,
        String note,
        Boolean manualBlock
) {
}
