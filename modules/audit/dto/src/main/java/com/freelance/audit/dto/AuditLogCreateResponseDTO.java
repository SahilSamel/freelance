package com.freelance.audit.dto;

import java.util.UUID;

/**
 * Response payload returned after an audit event is persisted.
 */
public record AuditLogCreateResponseDTO(
        UUID id,
        String traceId
) {
}
