package com.freelance.role.update;

import com.freelance.authorization.dto.role.update.RoleUpdateRequestDTO;
import com.freelance.authorization.dto.role.update.RoleUpdateResponseDTO;
import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.authorization.entity.Authorization;
import com.freelance.mapping.roleAuthorization.entity.RoleAuthorization;
import com.freelance.role.entity.Role;
import com.freelance.manager.CacheManager;
import com.freelance.authorization.repository.AuthorizationRepository;
import com.freelance.mapping.roleAuthorization.repository.RoleAuthorizationRepository;
import com.freelance.role.repository.RoleRepository;
import com.freelance.role.assembler.RoleAssembler;
import com.freelance.role.update.constraint.RoleUpdateConstraint;
import com.freelance.role.update.validation.RoleUpdateValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RoleUpdateService {

    private final RoleRepository roleRepository;
    private final RoleAuthorizationRepository roleAuthorizationRepository;
    private final AuthorizationRepository authorizationRepository;
    private final RoleAssembler roleAssembler;

    public RoleUpdateService() {
        this(new RoleRepository(), new RoleAuthorizationRepository(), new AuthorizationRepository(), new RoleAssembler());
    }

    public RoleUpdateService(
            RoleRepository roleRepository,
            RoleAuthorizationRepository roleAuthorizationRepository,
            AuthorizationRepository authorizationRepository,
            RoleAssembler roleAssembler
    ) {
        this.roleRepository = roleRepository;
        this.roleAuthorizationRepository = roleAuthorizationRepository;
        this.authorizationRepository = authorizationRepository;
        this.roleAssembler = roleAssembler;
    }

    public RoleUpdateResponseDTO updateRole(RoleUpdateRequestDTO request) {
        new RoleUpdateValidation().validateRequest(request);

        List<Authorization> newAuthorizations = new ArrayList<>();
        Role role = new RoleUpdateConstraint(roleRepository, authorizationRepository)
                .checkConstraints(request, newAuthorizations);

        if (request.name() != null && !request.name().isBlank()) {
            role.setName(request.name());
        }
        if (request.description() != null) {
            role.setDescription(request.description());
        }
        roleRepository.merge(role);

        List<Authorization> effectiveAuthorizations;
        if (request.authorizationIds() != null) {
            roleAuthorizationRepository.removeByRoleId(role.getId());
            List<RoleAuthorization> mappings = new ArrayList<>();
            for (Authorization auth : newAuthorizations) {
                mappings.add(new RoleAuthorization(role, auth));
            }
            if (!mappings.isEmpty()) {
                roleAuthorizationRepository.persistEntriesInBatch(mappings);
            }
            effectiveAuthorizations = newAuthorizations;
        } else {
            List<RoleAuthorization> existingMappings = roleAuthorizationRepository.getRoleAuthorizationsByRoleId(role.getId());
            effectiveAuthorizations = existingMappings.stream()
                    .map(RoleAuthorization::getAuthorization)
                    .filter(Objects::nonNull)
                    .toList();
        }

        try {
            CacheManager.getInstance().invalidate(RoleAuthorizationKeyCacheConfig.NAME, role.getId());
        } catch (Exception ignored) {}

        return roleAssembler.toUpdateResponseDTO(role, effectiveAuthorizations);
    }
}
