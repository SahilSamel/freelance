package com.freelance.auth.service.constraint;

import com.freelance.keys.authentication.session.SessionConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.user.read.User;

import java.util.UUID;

public class MeConstraint extends AbstractConstraint {

    private final User userService;

    public MeConstraint() {
        this(new User());
    }

    public MeConstraint(User userService) {
        this.userService = userService;
    }

    public void checkConstraints() {
        UUID userId = Transaction.getRequestContext().userId();
        if (userId == null) {
            raiseConstraintError(SessionConstraintErrors.INVALID_SESSION);
            return;
        }

        if (userService != null && !userService.existsById(userId)) {
            raiseConstraintError(SessionConstraintErrors.INVALID_SESSION);
        }
    }
}
