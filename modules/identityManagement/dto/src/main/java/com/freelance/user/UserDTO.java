package com.freelance.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String password,
        LocalDateTime createdDate,
        LocalDateTime lastUpdatedDate
) {
}
