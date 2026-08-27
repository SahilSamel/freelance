package com.freelance.identityManagement.mapping.userRole;

import java.util.UUID;

/**
 * Data transfer object representing a query request to locate all role IDs
 * assigned to a designated user.
 *
 * @param userId unique identifier of the user whose roles are to be queried
 */
public record UserRoleSearchRequestDTO(UUID userId) {
}
