package com.freelance.authorization.dto;

import java.util.List;
import java.util.UUID;

public record RoleDTO(
        UUID id,
        String name,
        String description,
        boolean isSystem,
        List<String> authorizationKeys
) {}
