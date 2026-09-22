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

        return assembler.fromEntity(existingUser);
    }
}
