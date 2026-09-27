package com.freelance.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String password,
        String firstName,
        String lastName,
        LocalDateTime createdDate,
        LocalDateTime lastUpdatedDate
) {
    public UserDTO(UUID id, String username, String password, LocalDateTime createdDate, LocalDateTime lastUpdatedDate) {
        this(id, username, password, null, null, createdDate, lastUpdatedDate);
    }
}
