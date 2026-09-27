package com.freelance.user.update;

import com.freelance.user.contact.UserContactResponseDTO;

import java.util.List;
import java.util.UUID;

public record UserUpdateResponseDTO(
        UUID id,
        String username,
        String firstName,
        String lastName,
        List<UserContactResponseDTO> contacts
) {
    public UserUpdateResponseDTO(UUID id, String username) {
        this(id, username, null, null, List.of());
    }

    public UserUpdateResponseDTO(UUID id, String username, List<UserContactResponseDTO> contacts) {
        this(id, username, null, null, contacts);
    }
}
