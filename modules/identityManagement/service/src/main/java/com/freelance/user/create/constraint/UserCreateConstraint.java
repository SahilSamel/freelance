package com.freelance.user.create.constraint;

import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.user.repository.UserRepository;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.create.UserCreateRequestDTO;

public class UserCreateConstraint extends AbstractConstraint {

    private final UserRepository userRepository;

    public UserCreateConstraint() {
        this(new UserRepository());
    }

    public UserCreateConstraint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void checkConstraints(UserCreateRequestDTO request) {
        if (request == null || request.username() == null) {
            return;
        }

        if (userRepository.existsByUsername(request.username())) {
            raiseConstraintError(UserConstraintErrors.USER_ALREADY_EXISTS);
        }
    }
}
