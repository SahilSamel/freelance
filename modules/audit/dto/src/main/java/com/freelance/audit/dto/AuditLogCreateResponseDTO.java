package com.freelance.audit.dto;

import java.util.UUID;

public record AuditLogCreateResponseDTO(
        UUID id,
        String traceId
) {
}
