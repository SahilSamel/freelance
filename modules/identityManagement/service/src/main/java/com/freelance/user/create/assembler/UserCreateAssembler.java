package com.freelance.user.create.assembler;

import com.freelance.entity.user.User;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.create.UserCreateResponseDTO;
import com.freelance.utility.password.HashUtility;

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
}
