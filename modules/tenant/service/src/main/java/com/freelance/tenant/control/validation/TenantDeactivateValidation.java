package com.freelance.tenant.control.validation;

import com.freelance.keys.tenant.TenantValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

import static com.freelance.tenant.onboarding.validation.TenantOnboardValidation.TENANT_PATTERN;

public class TenantDeactivateValidation extends AbstractValidation {

    public void validateRequest(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            raiseValidationError(TenantValidationErrors.INVALID_TENANT_ID, "tenantId");
        } else if (!TENANT_PATTERN.matcher(tenantId.trim()).matches()) {
            raiseValidationError(TenantValidationErrors.INVALID_TENANT_ID_FORMAT, "tenantId");
        }

        validate();
    }
}
