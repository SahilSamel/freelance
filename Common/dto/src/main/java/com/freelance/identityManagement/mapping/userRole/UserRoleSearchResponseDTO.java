package com.freelance.identityManagement.mapping.userRole;

import java.util.List;
import java.util.UUID;

/**
 * Data transfer object encapsulating the list of role IDs associated with a user.
 *
 * @param roleIds list of role unique identifiers assigned to the user; never null
 */
public record UserRoleSearchResponseDTO(List<UUID> roleIds) {
}
