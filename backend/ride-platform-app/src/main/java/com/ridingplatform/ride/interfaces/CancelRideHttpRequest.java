package com.ridingplatform.ride.interfaces;

import jakarta.validation.constraints.NotBlank;

public record CancelRideHttpRequest(
        @NotBlank String cancelReason
) {
}
