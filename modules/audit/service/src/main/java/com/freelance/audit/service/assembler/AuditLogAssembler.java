package com.freelance.audit.service.assembler;

import com.freelance.audit.dto.AuditLogCreateRequestDTO;
import com.freelance.audit.dto.AuditLogCreateResponseDTO;
import com.freelance.entity.audit.AuditLog;

public class AuditLogAssembler {

    public AuditLog toEntity(AuditLogCreateRequestDTO request) {
        if (request == null) {
            return null;
        }

        return new AuditLog(
                request.timestamp(),
                request.traceId(),
                request.userId(),
                null,
                request.httpMethod(),
                request.endpoint(),
                request.statusCode(),
                request.durationMs(),
                request.ipAddress(),
                request.userAgent(),
                request.authMechanism()
        );
    }

    public AuditLogCreateResponseDTO toResponse(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        return new AuditLogCreateResponseDTO(
                entity.getId(),
                entity.getTraceId()
        );
    }
}
