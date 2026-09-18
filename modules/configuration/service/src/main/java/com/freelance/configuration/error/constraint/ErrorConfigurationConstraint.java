package com.freelance.configuration.error.constraint;

import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateRequestDTO;
import com.freelance.configuration.error.entity.ErrorConfiguration;
import com.freelance.configuration.error.repository.ErrorConfigurationRepository;
import com.freelance.keys.configuration.error.ErrorConfigConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class ErrorConfigurationConstraint extends AbstractConstraint {

    private final ErrorConfigurationRepository repository;

    public ErrorConfigurationConstraint() {
        this(new ErrorConfigurationRepository());
    }

    public ErrorConfigurationConstraint(ErrorConfigurationRepository repository) {
        this.repository = repository;
    }

    public ErrorConfiguration checkConstraints(ErrorConfigurationUpdateRequestDTO request) {
        if (request == null || request.errorId() == null) {
            return null;
        }

        ErrorConfiguration existing = repository.get(request.errorId());
        if (existing == null) {
            raiseConstraintError(ErrorConfigConstraintErrors.ERROR_CONFIGURATION_NOT_FOUND);
        }

        return existing;
    }
}
