package com.freelance.authorization.dto.role.update;

import java.util.List;
import java.util.UUID;

public record RoleUpdateRequestDTO(
        UUID id,
        String name,
        String description,
        List<UUID> authorizationIds
) {}
