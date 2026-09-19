package com.freelance.user.create;

import java.util.List;
import java.util.UUID;

public record UserCreateResponseDTO(
        UUID id,
        String username,
        List<UUID> roleIds
) {
    public UserCreateResponseDTO(UUID id, String username) {
        this(id, username, List.of());
    }
}
