package com.freelance.user.create.assembler;

import com.freelance.user.entity.User;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.create.UserCreateResponseDTO;
import com.freelance.utility.password.HashUtility;

import java.util.List;
import java.util.UUID;

public class UserCreateAssembler {

    public User toEntity(UserCreateRequestDTO request) {
        if (request == null) return null;
        String hashedPassword = HashUtility.hash(request.password());
        return new User(request.username(), hashedPassword);
    }

    public UserCreateResponseDTO fromEntity(User user) {
        if (user == null) return null;
        return new UserCreateResponseDTO(user.getId(), user.getUsername());
    }

    public UserCreateResponseDTO fromEntity(User user, List<UUID> roleIds) {
        if (user == null) return null;
        return new UserCreateResponseDTO(user.getId(), user.getUsername(), roleIds != null ? roleIds : List.of());
    }
}

