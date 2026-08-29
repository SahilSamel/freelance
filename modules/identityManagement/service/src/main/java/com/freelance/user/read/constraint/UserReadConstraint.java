package com.freelance.user.read.constraint;

import com.freelance.entity.user.User;
import com.freelance.keys.identityManagement.ConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class UserReadConstraint extends AbstractConstraint {

    public void checkUserExists(User user) {
        if (user == null) {
            raiseConstraintError(ConstraintErrors.USER_NOT_FOUND);
        }
    }
}
