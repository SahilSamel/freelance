package com.freelance.user.read;

import com.freelance.repository.user.UserRepository;
import com.freelance.user.read.assembler.UserReadAssembler;

public class User {
    public UserReadResponseDTO read(UserReadRequestDTO userReadRequestDTO) {
        UserReadAssembler userReadAssembler = new UserReadAssembler();
        com.freelance.entity.user.User user = userReadAssembler.toEntity(userReadRequestDTO);

        UserRepository userRepository = new UserRepository();
        user = userRepository.get(user.getId());

        return userReadAssembler.fromEntity(user);
    }

    public UserReadResponseDTO findByUsername(UserReadRequestDTO userReadRequestDTO) {
        UserReadAssembler userReadAssembler = new UserReadAssembler();

        UserRepository userRepository = new UserRepository();
        com.freelance.entity.user.User user = userRepository.findByUsername(userReadRequestDTO.username()).orElse(null);
        return userReadAssembler.fromEntity(user);
    }
}
