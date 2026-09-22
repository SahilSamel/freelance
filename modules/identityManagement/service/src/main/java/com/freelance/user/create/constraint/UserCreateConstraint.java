package com.freelance.user.create.constraint;

import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.repository.UserRepository;

public class UserCreateConstraint extends AbstractConstraint {

    private final UserRepository userRepository;
    private final UserContactRepository userContactRepository;

    public UserCreateConstraint() {
        this(new UserRepository(), new UserContactRepository());
    }

    public UserCreateConstraint(UserRepository userRepository) {
        this(userRepository, new UserContactRepository());
    }

    public UserCreateConstraint(UserRepository userRepository, UserContactRepository userContactRepository) {
        this.userRepository = userRepository;
        this.userContactRepository = userContactRepository;
    }

    public void checkConstraints(UserCreateRequestDTO request) {
        if (request == null) {
            return;
        }

        if (request.username() != null && userRepository.existsByUsername(request.username())) {
            raiseConstraintError(UserConstraintErrors.USER_ALREADY_EXISTS);
        }

        if (request.contacts() != null && userContactRepository != null) {
            for (UserContactRequestDTO contact : request.contacts()) {
                if (contact != null && contact.channel() != null && contact.contactValue() != null && !contact.contactValue().isBlank()) {
                    if (userContactRepository.findByContactAndChannel(contact.contactValue(), contact.channel()).isPresent()) {
                        raiseConstraintError(UserConstraintErrors.CONTACT_ALREADY_EXISTS);
                    }
                }
            }
        }
    }
}
