package com.freelance.audit.service.validation;

import com.freelance.audit.dto.list.AuditLogListRequestDTO;
import com.freelance.keys.audit.http.HttpAuditValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class AuditLogListValidation extends AbstractValidation {

    public void validateRequest(AuditLogListRequestDTO request) {
        if (request == null) {
            return;
        }

        if (request.limit() != null && request.limit() <= 0) {
            raiseValidationError(HttpAuditValidationErrors.INVALID_LIMIT, "limit");
        }

        if (request.offset() != null && request.offset() < 0) {
            raiseValidationError(HttpAuditValidationErrors.INVALID_OFFSET, "offset");
        }

        if (request.fromTimestamp() != null && request.toTimestamp() != null) {
            if (request.fromTimestamp().isAfter(request.toTimestamp())) {
                raiseValidationError(HttpAuditValidationErrors.INVALID_DATE_RANGE, "fromTimestamp");
            }
        }

        validate();
    }
}
