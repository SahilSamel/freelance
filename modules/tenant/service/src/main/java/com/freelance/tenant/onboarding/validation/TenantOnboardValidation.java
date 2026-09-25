package com.freelance.tenant.onboarding.validation;

import com.freelance.keys.tenant.TenantValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.tenant.dto.TenantCreateRequestDTO;

import java.util.Set;
import java.util.regex.Pattern;

public class TenantOnboardValidation extends AbstractValidation {

    public static final Pattern TENANT_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{2,64}$");

    public static final Set<String> RESERVED_TENANT_NAMES = Set.of(
            "freelance",
            "public",
            "information_schema",
            "pg_catalog",
            "system",
            "admin",
            "control",
            "master"
    );

    public void validateRequest(TenantCreateRequestDTO request) {
        if (request == null) {
            raiseValidationError(TenantValidationErrors.INVALID_TENANT_ID, "tenantId");
            validate();
            return;
        }

        if (request.tenantId() == null || request.tenantId().isBlank()) {
            raiseValidationError(TenantValidationErrors.INVALID_TENANT_ID, "tenantId");
        } else {
            String trimmed = request.tenantId().trim();
            if (!TENANT_PATTERN.matcher(trimmed).matches()) {
                raiseValidationError(TenantValidationErrors.INVALID_TENANT_ID_FORMAT, "tenantId");
            }
            if (RESERVED_TENANT_NAMES.contains(trimmed.toLowerCase())) {
                raiseValidationError(TenantValidationErrors.RESERVED_TENANT_ID, "tenantId");
            }
        }

        if (request.name() != null && request.name().length() > 255) {
            raiseValidationError(TenantValidationErrors.INVALID_TENANT_NAME, "name");
        }

        validate();
    }
}
