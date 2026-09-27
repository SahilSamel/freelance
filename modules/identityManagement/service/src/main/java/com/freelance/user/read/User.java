package com.freelance.user.read;

import com.freelance.user.repository.UserRepository;
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
        com.freelance.user.entity.User user = userRepository.findByUsername(userReadRequestDTO.username()).orElse(null);
        return assembler.fromEntity(user);
    }

    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return userRepository.existsByUsername(username);
    }

    public boolean existsById(java.util.UUID id) {
        if (id == null) {
            return false;
        }
        return userRepository.findById(id).isPresent();
    }

    public java.util.Optional<com.freelance.user.UserDTO> findById(java.util.UUID id) {
        if (id == null) {
            return java.util.Optional.empty();
        }
        return userRepository.findById(id).map(u -> assembler.fromEntity(u).userDTO());
    }

    public java.util.List<com.freelance.user.UserDTO> readUsersByIds(java.util.List<java.util.UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.List.of();
        }
        return userRepository.findByIds(ids).stream()
                .map(u -> assembler.fromEntity(u).userDTO())
                .toList();
    }

    public UserReadResponseDTO readUser(UserReadRequestDTO request) {
        new UserReadValidation().validateRequest(request);

        com.freelance.user.entity.User user = null;
        if (request.id() != null) {
            user = userRepository.findById(request.id()).orElse(null);
        } else if (request.username() != null && !request.username().isBlank()) {
            user = userRepository.findByUsername(request.username()).orElse(null);
        }

        new UserReadConstraint().checkUserExists(user);

        return assembler.fromEntity(user);
    }
}
