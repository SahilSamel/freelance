package com.freelance.configuration.system.validation;

import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.keys.configuration.ValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class SystemConfigurationValidation extends AbstractValidation {

    public void validateUpdateRequest(SystemConfigurationUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(ValidationErrors.INVALID_CONFIG_ID, "configId");
            validate();
            return;
        }

        if (request.configId() == null || request.configId().isBlank()) {
            raiseValidationError(ValidationErrors.INVALID_CONFIG_ID, "configId");
        }

        if (request.configValue() == null) {
            raiseValidationError(ValidationErrors.INVALID_CONFIG_VALUE, "configValue");
        }

        validate();
    }
}
