package com.freelance.user.read;

import java.util.UUID;

public record UserReadRequestDTO(
        UUID id,
        String username
) {
}