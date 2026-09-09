package com.freelance.user.read.assembler;

import com.freelance.user.entity.User;
import com.freelance.user.UserDTO;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;

public class UserReadAssembler {

    public User toEntity(UserReadRequestDTO userReadRequestDTO) {
        if (userReadRequestDTO == null) return null;
        User user = new User();
        user.setId(userReadRequestDTO.id());
        user.setUsername(userReadRequestDTO.username());
        return user;
    }

    public UserReadResponseDTO fromEntity(User user) {
        if (user == null) return null;

        return new UserReadResponseDTO(
                new UserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getCreatedDate(),
                        user.getLastUpdatedDate()
                )
        );
    }
}
