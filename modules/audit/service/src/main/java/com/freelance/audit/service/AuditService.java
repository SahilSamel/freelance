package com.freelance.audit.service;

import com.freelance.audit.dto.AuditLogCreateRequestDTO;
import com.freelance.audit.dto.AuditLogCreateResponseDTO;
import com.freelance.audit.service.assembler.AuditLogAssembler;
import com.freelance.audit.service.validation.AuditValidation;
import com.freelance.entity.audit.AuditLog;
import com.freelance.repository.audit.AuditLogRepository;

public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogAssembler auditLogAssembler;

    public AuditService() {
        this.auditLogRepository = new AuditLogRepository();
        this.auditLogAssembler = new AuditLogAssembler();
    }

    public AuditLogCreateResponseDTO createAuditLog(AuditLogCreateRequestDTO request) {
        new AuditValidation().validateRequest(request);

        AuditLog entity = auditLogAssembler.toEntity(request);
        auditLogRepository.persist(entity);

        return auditLogAssembler.toResponse(entity);
    }
}
