package com.freelance.user.create;

import java.util.List;
import java.util.UUID;

public record UserCreateRequestDTO(
        String username,
        String password,
        List<UUID> roleIds
) {
    public UserCreateRequestDTO(String username, String password) {
        this(username, password, List.of());
    }
}
