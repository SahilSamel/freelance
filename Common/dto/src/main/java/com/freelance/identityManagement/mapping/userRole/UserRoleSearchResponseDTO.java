package com.freelance.identityManagement.mapping.userRole;

import java.util.List;
import java.util.UUID;

public record UserRoleSearchResponseDTO(List<UUID> roleIds) {
}
