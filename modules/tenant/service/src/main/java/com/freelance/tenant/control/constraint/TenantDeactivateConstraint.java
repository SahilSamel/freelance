package com.freelance.tenant.control.constraint;

import com.freelance.keys.tenant.TenantConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.system.SystemHardcoding;
import com.freelance.tenant.entity.TenantEntity;
import com.freelance.tenant.repository.TenantRepository;

import java.util.Objects;

public class TenantDeactivateConstraint extends AbstractConstraint {

    private final TenantRepository tenantRepository;

    public TenantDeactivateConstraint() {
        this(new TenantRepository());
    }

    public TenantDeactivateConstraint(TenantRepository tenantRepository) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository must not be null");
    }

    public TenantEntity checkConstraints(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }

        String normalizedTenantId = tenantId.trim().toLowerCase();
        if (SystemHardcoding.DEFAULT_TENANT.asString().equalsIgnoreCase(normalizedTenantId)) {
            raiseConstraintError(TenantConstraintErrors.CANNOT_DEACTIVATE_CONTROL_PLANE);
        }

        return tenantRepository.findByTenantId(normalizedTenantId)
                .orElseGet(() -> {
                    raiseConstraintError(TenantConstraintErrors.TENANT_NOT_FOUND);
                    return null;
                });
    }
}
