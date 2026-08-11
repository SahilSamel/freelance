package com.freelance.authorization.dto;

import java.util.List;

public record RoleUpdateRequestDTO(
        String name,
        String description,
        List<String> authorizationKeys
) {}
