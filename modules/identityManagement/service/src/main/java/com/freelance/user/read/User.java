package com.freelance.user.read;

import com.freelance.repository.user.UserRepository;
import com.freelance.user.read.assembler.UserReadAssembler;
import com.freelance.user.read.constraint.UserReadConstraint;
import com.freelance.user.read.validation.UserReadValidation;

public class User {

    private final UserRepository userRepository;
    private final UserReadAssembler assembler;

    public User() {
        this(new UserRepository(), new UserReadAssembler());
    }

    public User(UserRepository userRepository, UserReadAssembler assembler) {
        this.userRepository = userRepository;
        this.assembler = assembler;
    }

    public UserReadResponseDTO findByUsername(UserReadRequestDTO userReadRequestDTO) {
        if (userReadRequestDTO == null || userReadRequestDTO.username() == null) {
            return null;
        }
        com.freelance.entity.user.User user = userRepository.findByUsername(userReadRequestDTO.username()).orElse(null);
        return assembler.fromEntity(user);
    }

    public UserReadResponseDTO readUser(UserReadRequestDTO request) {
        new UserReadValidation().validateRequest(request);

        com.freelance.entity.user.User user = null;
        if (request.id() != null) {
            user = userRepository.findById(request.id()).orElse(null);
        } else if (request.username() != null && !request.username().isBlank()) {
            user = userRepository.findByUsername(request.username()).orElse(null);
        }

        new UserReadConstraint().checkUserExists(user);

        return assembler.fromEntity(user);
    }
}
