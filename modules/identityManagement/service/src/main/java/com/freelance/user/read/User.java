package com.freelance.user.read;

import com.freelance.repository.user.UserRepository;
import com.freelance.user.read.assembler.UserReadAssembler;

public class User {

    public UserReadResponseDTO findByUsername(UserReadRequestDTO userReadRequestDTO) {
        UserReadAssembler userReadAssembler = new UserReadAssembler();

        UserRepository userRepository = new UserRepository();
        com.freelance.entity.user.User user = userRepository.findByUsername(userReadRequestDTO.username()).orElse(null);
        return userReadAssembler.fromEntity(user);
    }
}
