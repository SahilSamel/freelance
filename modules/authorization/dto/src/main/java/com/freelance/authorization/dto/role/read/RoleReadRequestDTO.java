package com.freelance.authorization.dto.role.read;

import java.util.UUID;

public record RoleReadRequestDTO(
        UUID id,
        String name
) {}
