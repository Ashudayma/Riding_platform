package com.ridingplatform.admin.interfaces;

import java.time.Instant;
import java.util.UUID;

public record AdminAuditLogHttpResponse(
        UUID auditLogId,
        String actionCode,
        String targetType,
        UUID targetId,
        String resultStatus,
        String requestId,
        String traceId,
        Instant occurredAt
) {
}
