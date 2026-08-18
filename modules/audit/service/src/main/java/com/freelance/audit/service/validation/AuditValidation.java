package com.freelance.audit.service.validation;

import com.freelance.audit.dto.AuditLogCreateRequestDTO;
import com.freelance.error.audit.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class AuditValidation extends AbstractValidation {

    public void validateRequest(AuditLogCreateRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.MISSING_HTTP_METHOD, "request");
            validate();
            return;
        }

        if (request.httpMethod() == null || request.httpMethod().isBlank()) {
            raiseValidationError(ValidationErrors.MISSING_HTTP_METHOD, "httpMethod");
        }

        if (request.endpoint() == null || request.endpoint().isBlank()) {
            raiseValidationError(ValidationErrors.MISSING_ENDPOINT, "endpoint");
        }

        if (request.statusCode() <= 0) {
            raiseValidationError(ValidationErrors.MISSING_STATUS_CODE, "statusCode");
        }

        if (request.timestamp() == null) {
            raiseValidationError(ValidationErrors.MISSING_TIMESTAMP, "timestamp");
        }

        if (request.durationMs() < 0) {
            raiseValidationError(ValidationErrors.INVALID_DURATION, "durationMs");
        }

        if (request.traceId() == null || request.traceId().isBlank()) {
            raiseValidationError(ValidationErrors.MISSING_TRACE_ID, "traceId");
        }

        validate();
    }
}
