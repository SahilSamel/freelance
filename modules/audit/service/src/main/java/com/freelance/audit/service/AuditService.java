package com.freelance.audit.service;

import com.freelance.audit.dto.AuditLogCreateRequestDTO;
import com.freelance.audit.dto.AuditLogCreateResponseDTO;
import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.audit.dto.list.AuditLogListResponseDTO;
import com.freelance.audit.service.assembler.AuditLogAssembler;
import com.freelance.audit.service.validation.AuditLogListValidation;
import com.freelance.audit.service.validation.AuditValidation;
import com.freelance.audit.entity.AuditLog;
import com.freelance.audit.repository.AuditLogRepository;

import java.util.List;

import com.freelance.ConfigurationEngine;
import com.freelance.keys.audit.http.HttpAuditConfigKeys;

public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogAssembler auditLogAssembler;

    public AuditService() {
        this(new AuditLogRepository(), new AuditLogAssembler());
    }

    public AuditService(AuditLogRepository auditLogRepository, AuditLogAssembler auditLogAssembler) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogAssembler = auditLogAssembler;
    }

    public AuditLogCreateResponseDTO createAuditLog(AuditLogCreateRequestDTO request) {
        if (!ConfigurationEngine.getBoolean(HttpAuditConfigKeys.AUDITING_ENABLED)) {
            return null;
        }

        new AuditValidation().validateRequest(request);

        AuditLog entity = auditLogAssembler.toEntity(request);
        auditLogRepository.persist(entity);

        return auditLogAssembler.toResponse(entity);
    }

    public AuditLogListResponseDTO listAuditLogs(AuditLogListRequestDTO request) {
        new AuditLogListValidation().validateRequest(request);

        List<AuditLog> logs = auditLogRepository.findAuditLogs(request);
        return auditLogAssembler.toListResponse(logs);
    }
}
