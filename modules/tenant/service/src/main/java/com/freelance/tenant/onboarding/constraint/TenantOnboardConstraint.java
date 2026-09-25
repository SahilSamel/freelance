package com.freelance.tenant.onboarding.constraint;

import com.freelance.keys.tenant.TenantConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.tenant.dto.TenantCreateRequestDTO;
import com.freelance.tenant.repository.TenantRepository;

import java.util.Objects;

public class TenantOnboardConstraint extends AbstractConstraint {

    private final TenantRepository tenantRepository;

    public TenantOnboardConstraint() {
        this(new TenantRepository());
    }

    public TenantOnboardConstraint(TenantRepository tenantRepository) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
    }

    public void checkConstraints(TenantCreateRequestDTO request) {
        if (request == null || request.tenantId() == null || request.tenantId().isBlank()) {
            return;
        }

        String tenantId = request.tenantId().trim().toLowerCase();
        if (tenantRepository.findByTenantId(tenantId).isPresent()) {
            raiseConstraintError(TenantConstraintErrors.TENANT_ALREADY_EXISTS);
        }
    }
}
