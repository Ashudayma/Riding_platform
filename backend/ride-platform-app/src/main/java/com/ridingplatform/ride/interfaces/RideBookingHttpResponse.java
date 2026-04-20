package com.ridingplatform.ride.interfaces;

import com.ridingplatform.ride.application.RideBookingStatus;
import com.ridingplatform.ride.domain.RideType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RideBookingHttpResponse(
        UUID rideRequestId,
        UUID rideId,
        UUID riderProfileId,
        RideType rideType,
        RideBookingStatus status,
        UUID fareQuoteId,
        BigDecimal estimatedTotalAmount,
        String currencyCode,
        Instant requestedAt,
        List<RideStopHttpResponse> stops
) {
}
