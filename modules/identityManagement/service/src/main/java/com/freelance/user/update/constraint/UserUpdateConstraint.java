package com.freelance.user.update.constraint;

import com.freelance.user.entity.User;
import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.user.repository.UserRepository;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.update.UserUpdateRequestDTO;

import java.util.Optional;

public class UserUpdateConstraint extends AbstractConstraint {

    private final UserRepository userRepository;

    public UserUpdateConstraint() {
        this(new UserRepository());
    }

    public UserUpdateConstraint(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User checkConstraints(UserUpdateRequestDTO request) {
        if (request == null || request.id() == null) {
            return null;
        }

        User existingUser = userRepository.findById(request.id())
                .orElse(null);

        if (existingUser == null) {
            raiseConstraintError(UserConstraintErrors.USER_TO_UPDATE_NOT_FOUND);
        }

        if (request.username() != null && !request.username().isBlank()) {
            if (!request.username().equals(existingUser.getUsername())) {
                Optional<User> byUsername = userRepository.findByUsername(request.username());
                if (byUsername.isPresent() && !byUsername.get().getId().equals(request.id())) {
                    raiseConstraintError(UserConstraintErrors.USER_ALREADY_EXISTS);
                }
            }
        }

        return existingUser;
    }
}
