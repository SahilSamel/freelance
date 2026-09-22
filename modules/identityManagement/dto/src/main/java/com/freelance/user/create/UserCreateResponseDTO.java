package com.freelance.user.create;

import com.freelance.user.contact.UserContactResponseDTO;

import java.util.List;
import java.util.UUID;

public record UserCreateResponseDTO(
        UUID id,
        String username,
        List<UUID> roleIds,
        List<UserContactResponseDTO> contacts
) {
    public UserCreateResponseDTO(UUID id, String username) {
        this(id, username, List.of(), List.of());
    }

    public UserCreateResponseDTO(UUID id, String username, List<UUID> roleIds) {
        this(id, username, roleIds != null ? roleIds : List.of(), List.of());
    }

    public UserCreateResponseDTO(UUID id, String username, List<UUID> roleIds, List<UserContactResponseDTO> contacts) {
        this.id = id;
        this.username = username;
        this.roleIds = roleIds != null ? roleIds : List.of();
        this.contacts = contacts != null ? contacts : List.of();
    }
}
