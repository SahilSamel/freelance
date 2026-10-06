package com.freelance.auth.dto;

import java.util.List;
import java.util.UUID;

public record AuthMeResponseDTO(
        UUID userId,
        String username,
        String firstName,
        String lastName,
        List<String> roles,
        List<String> permissions
) {
}
