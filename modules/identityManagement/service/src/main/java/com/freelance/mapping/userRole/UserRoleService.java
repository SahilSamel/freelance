package com.freelance.mapping.userRole;

import com.freelance.identityManagement.mapping.userRole.UserRoleSearchRequestDTO;
import com.freelance.identityManagement.mapping.userRole.UserRoleSearchResponseDTO;
import com.freelance.repository.mapping.userRole.UserRoleRepository;

import java.util.List;
import java.util.UUID;

public class UserRoleService {

    private final UserRoleRepository userRoleRepository;

    public UserRoleService() {
        this.userRoleRepository = new UserRoleRepository();
    }

    public UserRoleSearchResponseDTO fetchAssignedRoles(UserRoleSearchRequestDTO userRoleSearchRequestDTO) {
        List<UUID> roleIds = userRoleRepository.findRoleIdsByUserId(userRoleSearchRequestDTO.userId());
        return new UserRoleSearchResponseDTO(roleIds);
    }
}
