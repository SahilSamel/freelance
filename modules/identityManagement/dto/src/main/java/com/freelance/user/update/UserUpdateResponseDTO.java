package com.freelance.user.update;

import java.util.UUID;

public record UserUpdateResponseDTO(
        UUID id,
        String username
) {
}
