package com.freelance.role.delete;

import com.freelance.authorization.dto.role.delete.RoleDeleteRequestDTO;
import com.freelance.authorization.dto.role.delete.RoleDeleteResponseDTO;
import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.role.entity.Role;
import com.freelance.manager.CacheManager;
import com.freelance.mapping.roleAuthorization.repository.RoleAuthorizationRepository;
import com.freelance.role.repository.RoleRepository;
import com.freelance.role.delete.constraint.RoleDeleteConstraint;
import com.freelance.role.delete.validation.RoleDeleteValidation;

public class RoleDeleteService {

    private final RoleRepository roleRepository;
    private final RoleAuthorizationRepository roleAuthorizationRepository;

    public RoleDeleteService() {
        this(new RoleRepository(), new RoleAuthorizationRepository());
    }

    public RoleDeleteService(RoleRepository roleRepository, RoleAuthorizationRepository roleAuthorizationRepository) {
        this.roleRepository = roleRepository;
        this.roleAuthorizationRepository = roleAuthorizationRepository;
    }

    public RoleDeleteResponseDTO deleteRole(RoleDeleteRequestDTO request) {
        new RoleDeleteValidation().validateRequest(request);
        Role role = new RoleDeleteConstraint(roleRepository).checkConstraints(request);

        roleAuthorizationRepository.removeByRoleId(role.getId());
        roleRepository.remove(role);

        try {
            CacheManager.invalidate(RoleAuthorizationKeyCacheConfig.NAME, role.getId());
        } catch (Exception ignored) {}

        return new RoleDeleteResponseDTO(role.getId(), true);
    }
}
