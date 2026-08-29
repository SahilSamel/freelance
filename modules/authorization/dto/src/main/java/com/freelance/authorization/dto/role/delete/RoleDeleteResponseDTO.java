package com.freelance.authorization.dto.role.delete;

import java.util.UUID;

public record RoleDeleteResponseDTO(
        UUID id,
        boolean deleted
) {}
