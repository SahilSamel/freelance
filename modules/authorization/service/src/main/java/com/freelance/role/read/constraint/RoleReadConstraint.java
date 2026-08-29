package com.freelance.role.read.constraint;

import com.freelance.entity.role.Role;
import com.freelance.keys.authorization.ConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class RoleReadConstraint extends AbstractConstraint {

    public void checkRoleExists(Role role) {
        if (role == null) {
            raiseConstraintError(ConstraintErrors.ROLE_NOT_FOUND);
        }
    }
}
