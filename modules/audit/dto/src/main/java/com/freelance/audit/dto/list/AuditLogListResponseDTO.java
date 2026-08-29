package com.freelance.audit.dto.list;

import com.freelance.audit.dto.AuditLogDTO;
import java.util.List;

public record AuditLogListResponseDTO(
        List<AuditLogDTO> auditLogs,
        int totalCount
) {}
