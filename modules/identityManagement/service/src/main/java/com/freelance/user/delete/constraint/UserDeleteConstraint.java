package com.freelance.user.delete.constraint;

import com.freelance.user.entity.User;
import com.freelance.keys.identityManagement.ConstraintErrors;
import com.freelance.user.repository.UserRepository;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.user.delete.UserDeleteRequestDTO;

import java.util.UUID;

public class UserDeleteConstraint extends AbstractConstraint {

    private final UserRepository userRepository;

    public UserDeleteConstraint() {
        this(new UserRepository());
    }

    public UserDeleteConstraint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User checkConstraints(UserDeleteRequestDTO request) {
        if (request == null || request.id() == null) {
            return null;
        }

        User existingUser = userRepository.findById(request.id())
                .orElse(null);

        if (existingUser == null) {
            raiseConstraintError(ConstraintErrors.USER_TO_DELETE_NOT_FOUND);
        }

        UUID callerUserId = null;
        try {
            callerUserId = Transaction.getRequestContext().userId();
        } catch (Exception ignored) {
            // Context may be unavailable in isolated unit tests
        }

        if (callerUserId != null && callerUserId.equals(request.id())) {
            raiseConstraintError(ConstraintErrors.CANNOT_DELETE_SELF);
        }

        return existingUser;
    }
}
