package com.freelance.user.create;

import java.util.UUID;

public record UserCreateResponseDTO(
        UUID id,
        String username
) {
}
