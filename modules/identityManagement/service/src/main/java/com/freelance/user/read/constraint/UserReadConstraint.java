package com.freelance.user.read.constraint;

import com.freelance.user.entity.User;
import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class UserReadConstraint extends AbstractConstraint {

    public void checkUserExists(User user) {
        if (user == null) {
            raiseConstraintError(UserConstraintErrors.USER_NOT_FOUND);
        }
    }
}
