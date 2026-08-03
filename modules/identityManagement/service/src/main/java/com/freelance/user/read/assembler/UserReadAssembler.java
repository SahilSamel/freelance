package com.freelance.user.read.assembler;

import com.freelance.entity.user.User;
import com.freelance.user.UserDTO;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;

import java.util.UUID;

public class UserReadAssembler {

    public User toEntity(UserReadRequestDTO userReadRequestDTO) {
        User user = new User();
        user.setId(UUID.fromString(userReadRequestDTO.userId()));
        user.setUsername(userReadRequestDTO.username());
        return user;
    }

    public UserReadResponseDTO fromEntity(User user) {
        if (user == null) return null;

        return new UserReadResponseDTO(
                new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPassword(),
                        user.getCreatedAt()
                )
        );

    }
}
