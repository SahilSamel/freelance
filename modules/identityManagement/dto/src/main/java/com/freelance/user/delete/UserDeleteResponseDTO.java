package com.freelance.user.delete;

import java.util.UUID;

public record UserDeleteResponseDTO(
        UUID id,
        boolean deleted
) {
}
