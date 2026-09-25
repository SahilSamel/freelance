package com.freelance.user.update;

import com.freelance.alerts.preference.repository.UserContactRepository;
import com.freelance.user.entity.User;
import com.freelance.user.repository.UserRepository;
import com.freelance.user.update.assembler.UserUpdateAssembler;
import com.freelance.user.update.constraint.UserUpdateConstraint;
import com.freelance.user.update.validation.UserUpdateValidation;

public class UserUpdateService {

    private final UserRepository userRepository;
    private final UserContactRepository userContactRepository;
    private final UserUpdateAssembler assembler;

    public UserUpdateService() {
        this(new UserRepository(), new UserContactRepository(), new UserUpdateAssembler());
    }

    public UserUpdateService(UserRepository userRepository, UserUpdateAssembler assembler) {
        this(userRepository, new UserContactRepository(), assembler);
    }

    public UserUpdateService(UserRepository userRepository, UserContactRepository userContactRepository, UserUpdateAssembler assembler) {
        this.userRepository = userRepository;
        this.userContactRepository = userContactRepository;
        this.assembler = assembler;
    }

    public UserUpdateResponseDTO updateUser(UserUpdateRequestDTO request) {
        new UserUpdateValidation().validateRequest(request);
        User existingUser = new UserUpdateConstraint(userRepository, userContactRepository).checkConstraints(request);

        assembler.applyUpdates(existingUser, request);
        userRepository.merge(existingUser);

        if (request != null && request.contacts() != null) {
            java.util.List<com.freelance.alerts.preference.entity.UserContactEntity> dbContacts = userContactRepository.findByUserId(existingUser.getId());
            java.util.Set<java.util.UUID> updatedContactIds = new java.util.HashSet<>();
            if (existingUser.getContacts() != null) {
                for (var contact : existingUser.getContacts()) {
                    contact.setUserId(existingUser.getId());
                    var saved = userContactRepository.merge(contact);
                    if (saved != null && saved.getId() != null) {
                        updatedContactIds.add(saved.getId());
                    } else if (contact.getId() != null) {
                        updatedContactIds.add(contact.getId());
                    }
                }
            }
            if (dbContacts != null) {
                for (var dbContact : dbContacts) {
                    if (dbContact != null && dbContact.getId() != null && !updatedContactIds.contains(dbContact.getId())) {
                        userContactRepository.remove(dbContact);
                    }
                }
            }
        }

        return assembler.fromEntity(existingUser);
    }
}
