package com.freelance.authorization.dto;

import java.util.List;

public record RoleCreateRequestDTO(
        String name,
        String description,
        List<String> authorizationKeys
) {}
