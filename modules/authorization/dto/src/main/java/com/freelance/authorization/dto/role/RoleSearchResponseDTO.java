package com.freelance.authorization.dto.role;

import java.util.List;
import java.util.UUID;

public record RoleSearchResponseDTO(List<UUID> roleIds) {
}
