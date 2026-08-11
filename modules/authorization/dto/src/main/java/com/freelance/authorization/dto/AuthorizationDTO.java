package com.freelance.authorization.dto;

import java.util.UUID;

public record AuthorizationDTO(
        UUID id,
        String key,
        String name,
        String group
) {}
