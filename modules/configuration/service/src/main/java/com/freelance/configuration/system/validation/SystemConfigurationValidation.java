package com.freelance.configuration.system.validation;

import com.freelance.ConfigurationEngine;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.keys.configuration.system.SystemConfigValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class SystemConfigurationValidation extends AbstractValidation {

    public void validateUpdateRequest(SystemConfigurationUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(SystemConfigValidationErrors.INVALID_CONFIG_ID, "configId");
            validate();
            return;
        }

        if (request.configId() == null || request.configId().isBlank()) {
            raiseValidationError(SystemConfigValidationErrors.INVALID_CONFIG_ID, "configId");
        }

        if (request.configValue() == null) {
            raiseValidationError(SystemConfigValidationErrors.INVALID_CONFIG_VALUE, "configValue");
        }

        if (request.configId() != null && request.configValue() != null) {
            if (!ConfigurationEngine.validate(request.configId(), request.configValue())) {
                raiseValidationError(SystemConfigValidationErrors.CONFIG_VALUE_VALIDATION_FAILED, "configValue");
            }
        }

        validate();
    }
}
