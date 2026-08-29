package com.freelance.user.update;

import com.freelance.entity.user.User;
import com.freelance.repository.user.UserRepository;
import com.freelance.user.update.assembler.UserUpdateAssembler;
import com.freelance.user.update.constraint.UserUpdateConstraint;
import com.freelance.user.update.validation.UserUpdateValidation;

public class UserUpdateService {

    private final UserRepository userRepository;
    private final UserUpdateAssembler assembler;

    public UserUpdateService() {
        this(new UserRepository(), new UserUpdateAssembler());
    }

    public UserUpdateService(UserRepository userRepository, UserUpdateAssembler assembler) {
        this.userRepository = userRepository;
        this.assembler = assembler;
    }

    public UserUpdateResponseDTO updateUser(UserUpdateRequestDTO request) {
        new UserUpdateValidation().validateRequest(request);
        User existingUser = new UserUpdateConstraint(userRepository).checkConstraints(request);

        assembler.applyUpdates(existingUser, request);
        userRepository.merge(existingUser);

        return assembler.fromEntity(existingUser);
    }
}
