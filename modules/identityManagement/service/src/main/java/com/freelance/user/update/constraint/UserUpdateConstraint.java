package com.freelance.user.update.constraint;

import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.entity.User;
import com.freelance.keys.identityManagement.user.UserConstraintErrors;
import com.freelance.user.repository.UserRepository;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.update.UserUpdateRequestDTO;

import java.util.Optional;

public class UserUpdateConstraint extends AbstractConstraint {

    private final UserRepository userRepository;
    private final UserContactRepository userContactRepository;

    public UserUpdateConstraint() {
        this(new UserRepository(), new UserContactRepository());
    }

    public UserUpdateConstraint(UserRepository userRepository) {
        this(userRepository, new UserContactRepository());
    }

    public UserUpdateConstraint(UserRepository userRepository, UserContactRepository userContactRepository) {
        this.userRepository = userRepository;
        this.userContactRepository = userContactRepository;
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

        if (request.contacts() != null && userContactRepository != null) {
            for (UserContactRequestDTO contact : request.contacts()) {
                if (contact != null && contact.channel() != null && contact.contactValue() != null && !contact.contactValue().isBlank()) {
                    var existingContactOpt = userContactRepository.findByContactAndChannel(contact.contactValue(), contact.channel());
                    if (existingContactOpt.isPresent()) {
                        var existingContact = existingContactOpt.get();
                        if (existingContact.getUserId() != null && !existingContact.getUserId().equals(request.id())) {
                            raiseConstraintError(UserConstraintErrors.CONTACT_ALREADY_EXISTS);
                        }
                    }
                }
            }
        }

        return existingUser;
    }
}
