package com.freelance.user.update;

import java.util.UUID;

public record UserUpdateRequestDTO(
        UUID id,
        String username,
        String password
) {
}
