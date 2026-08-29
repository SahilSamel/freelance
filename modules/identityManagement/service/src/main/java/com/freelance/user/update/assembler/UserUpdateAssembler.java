package com.freelance.user.update.assembler;

import com.freelance.entity.user.User;
import com.freelance.user.update.UserUpdateRequestDTO;
import com.freelance.user.update.UserUpdateResponseDTO;
import com.freelance.utility.password.HashUtility;

public class UserUpdateAssembler {

    public void applyUpdates(User user, UserUpdateRequestDTO request) {
        if (user == null || request == null) return;

        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(HashUtility.hash(request.password()));
        }
    }

    public UserUpdateResponseDTO fromEntity(User user) {
        if (user == null) return null;
        return new UserUpdateResponseDTO(user.getId(), user.getUsername());
    }
}
