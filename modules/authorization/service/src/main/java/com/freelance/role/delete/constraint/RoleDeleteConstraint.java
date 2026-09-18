package com.freelance.role.delete.constraint;

import com.freelance.authorization.dto.role.delete.RoleDeleteRequestDTO;
import com.freelance.role.entity.Role;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.role.repository.RoleRepository;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class RoleDeleteConstraint extends AbstractConstraint {

    private final RoleRepository roleRepository;

    public RoleDeleteConstraint() {
        this(new RoleRepository());
    }

    public RoleDeleteConstraint(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role checkConstraints(RoleDeleteRequestDTO request) {
        if (request == null || request.id() == null) {
            return null;
        }

        Role existingRole = roleRepository.findById(request.id()).orElse(null);
        if (existingRole == null) {
            raiseConstraintError(RoleConstraintErrors.ROLE_TO_DELETE_NOT_FOUND);
        }

        if (existingRole.isSystem()) {
            raiseConstraintError(RoleConstraintErrors.SYSTEM_ROLE_CANNOT_BE_MANAGED);
        }

        return existingRole;
    }
}
