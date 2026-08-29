package com.freelance.audit.dto.list;

import java.time.Instant;
import java.util.UUID;

public record AuditLogListRequestDTO(
        UUID userId,
        String traceId,
        String httpMethod,
        String endpoint,
        Integer statusCode,
        Instant fromTimestamp,
        Instant toTimestamp,
        Integer limit,
        Integer offset
) {
    public static AuditLogListRequestDTO empty() {
        return new AuditLogListRequestDTO(null, null, null, null, null, null, null, null, null);
    }
}
