package com.freelance.authorization.dto.role.create;

import com.freelance.authorization.dto.AuthorizationDTO;
import java.util.List;
import java.util.UUID;

public record RoleCreateResponseDTO(
        UUID id,
        String name,
        String description,
        boolean isSystem,
        List<AuthorizationDTO> authorizations
) {}
