package com.freelance.authorization.dto.role.list;

import com.freelance.authorization.dto.role.RoleDTO;
import java.util.List;

public record RoleListResponseDTO(
        List<RoleDTO> roles
) {}
