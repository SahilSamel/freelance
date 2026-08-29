package com.freelance.authorization.dto.role.create;

import java.util.List;
import java.util.UUID;

public record RoleCreateRequestDTO(
        String name,
        String description,
        List<UUID> authorizationIds
) {}
