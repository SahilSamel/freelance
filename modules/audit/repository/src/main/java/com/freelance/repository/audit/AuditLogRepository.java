package com.freelance.repository.audit;

import com.freelance.entity.audit.AuditLog;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.UUID;

public class AuditLogRepository extends AbstractRepository<AuditLog, UUID> {

    public AuditLogRepository() {
        super();
    }
}
