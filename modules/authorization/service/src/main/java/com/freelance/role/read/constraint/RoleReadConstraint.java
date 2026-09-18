package com.freelance.role.read.constraint;

import com.freelance.role.entity.Role;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class RoleReadConstraint extends AbstractConstraint {

    public void checkRoleExists(Role role) {
        if (role == null) {
            raiseConstraintError(RoleConstraintErrors.ROLE_NOT_FOUND);
        }
    }
}
