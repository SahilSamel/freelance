package com.freelance.user.update;

import com.freelance.user.contact.UserContactResponseDTO;

import java.util.List;
import java.util.UUID;

public record UserUpdateResponseDTO(
        UUID id,
        String username,
        List<UserContactResponseDTO> contacts
) {
    public UserUpdateResponseDTO(UUID id, String username) {
        this(id, username, List.of());
    }
}
