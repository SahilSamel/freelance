package com.freelance.audit.service.assembler;

import com.freelance.audit.dto.AuditLogCreateRequestDTO;
import com.freelance.audit.dto.AuditLogCreateResponseDTO;
import com.freelance.audit.dto.AuditLogDTO;
import com.freelance.audit.dto.list.AuditLogListResponseDTO;
import com.freelance.audit.entity.AuditLog;

import java.util.Collections;
import java.util.List;

public class AuditLogAssembler {

    public AuditLog toEntity(AuditLogCreateRequestDTO request) {
        if (request == null) {
            return null;
        }

        return new AuditLog(
                request.timestamp(),
                request.traceId(),
                request.userId(),
                request.httpMethod(),
                request.endpoint(),
                request.requestData(),
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

    public AuditLogDTO toDTO(AuditLog entity) {
        if (entity == null) {
            return null;
        }

        return new AuditLogDTO(
                entity.getId(),
                entity.getTimestamp(),
                entity.getTraceId(),
                entity.getUserId(),
                entity.getHttpMethod(),
                entity.getEndpoint(),
                entity.getRequestData(),
                entity.getStatusCode(),
                entity.getDurationMs(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getAuthMechanism()
        );
    }

    public AuditLogListResponseDTO toListResponse(List<AuditLog> entities) {
        if (entities == null || entities.isEmpty()) {
            return new AuditLogListResponseDTO(Collections.emptyList(), 0);
        }

        List<AuditLogDTO> dtos = entities.stream()
                .map(this::toDTO)
                .toList();

        return new AuditLogListResponseDTO(dtos, dtos.size());
    }
}
