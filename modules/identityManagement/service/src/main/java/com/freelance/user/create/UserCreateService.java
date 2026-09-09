package com.freelance.user.create;

import com.freelance.user.entity.User;
import com.freelance.user.repository.UserRepository;
import com.freelance.user.create.assembler.UserCreateAssembler;
import com.freelance.user.create.constraint.UserCreateConstraint;
import com.freelance.user.create.validation.UserCreateValidation;

public class UserCreateService {

    private final UserRepository userRepository;
    private final UserCreateAssembler assembler;

    public UserCreateService() {
        this(new UserRepository(), new UserCreateAssembler());
    }

    public UserCreateService(UserRepository userRepository, UserCreateAssembler assembler) {
        this.userRepository = userRepository;
        this.assembler = assembler;
    }

    public UserCreateResponseDTO createUser(UserCreateRequestDTO request) {
        new UserCreateValidation().validateRequest(request);
        new UserCreateConstraint(userRepository).checkConstraints(request);

        User user = assembler.toEntity(request);
        userRepository.persist(user);

        return assembler.fromEntity(user);
    }
}
