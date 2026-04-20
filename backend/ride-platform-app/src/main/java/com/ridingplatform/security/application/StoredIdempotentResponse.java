package com.ridingplatform.security.application;

public record StoredIdempotentResponse(
        int httpStatus,
        String body
) {
}
