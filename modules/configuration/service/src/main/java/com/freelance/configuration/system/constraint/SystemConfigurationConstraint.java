package com.freelance.configuration.system.constraint;

import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.system.entity.SystemConfiguration;
import com.freelance.configuration.system.repository.SystemConfigurationRepository;
import com.freelance.keys.configuration.ConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class SystemConfigurationConstraint extends AbstractConstraint {

    private final SystemConfigurationRepository repository;

    public SystemConfigurationConstraint() {
        this(new SystemConfigurationRepository());
    }

    public SystemConfigurationConstraint(SystemConfigurationRepository repository) {
        this.repository = repository;
    }

    public SystemConfiguration checkConstraints(SystemConfigurationUpdateRequestDTO request) {
        if (request == null || request.configId() == null) {
            return null;
        }

        SystemConfiguration existing = repository.get(request.configId());
        if (existing == null) {
            raiseConstraintError(ConstraintErrors.CONFIGURATION_NOT_FOUND);
        }

        return existing;
    }
}
