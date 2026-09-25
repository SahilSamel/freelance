package com.freelance.tenant.control.constraint;

import com.freelance.keys.tenant.TenantConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.rest.jaxrs.tenant.TenantRegistry;
import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.repository.TenantRepository;

import java.util.Objects;

public class TenantSupportSessionConstraint extends AbstractConstraint {

    private final TenantRepository tenantRepository;

    public TenantSupportSessionConstraint() {
        this(new TenantRepository());
    }

    public TenantSupportSessionConstraint(TenantRepository tenantRepository) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
    }

    public void checkConstraints(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }

        String normalizedTenantId = tenantId.trim().toLowerCase();
        TenantEntity entity = tenantRepository.findByTenantId(normalizedTenantId)
                .orElseGet(() -> {
                    raiseConstraintError(TenantConstraintErrors.TENANT_NOT_FOUND);
                    return null;
                });

        if (!TenantRegistry.STATUS_ACTIVE.equalsIgnoreCase(entity.getStatus())) {
            raiseConstraintError(TenantConstraintErrors.TENANT_NOT_ACTIVE);
        }
    }
}
