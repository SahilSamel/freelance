package com.freelance.role.create;

import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.authorization.dto.role.create.RoleCreateResponseDTO;
import com.freelance.cache.config.RoleAuthorizationKeyCacheConfig;
import com.freelance.entity.authorization.Authorization;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorization;
import com.freelance.entity.role.Role;
import com.freelance.manager.CacheManager;
import com.freelance.repository.authorization.AuthorizationRepository;
import com.freelance.repository.mapping.roleAuthorization.RoleAuthorizationRepository;
import com.freelance.repository.role.RoleRepository;
import com.freelance.role.assembler.RoleAssembler;
import com.freelance.role.create.constraint.RoleCreateConstraint;
import com.freelance.role.create.validation.RoleCreateValidation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoleCreateService {

    private final RoleRepository roleRepository;
    private final RoleAuthorizationRepository roleAuthorizationRepository;
    private final AuthorizationRepository authorizationRepository;
    private final RoleAssembler roleAssembler;

    public RoleCreateService() {
        this(new RoleRepository(), new RoleAuthorizationRepository(), new AuthorizationRepository(), new RoleAssembler());
    }

    public RoleCreateService(
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

    public RoleCreateResponseDTO createRole(RoleCreateRequestDTO request) {
        new RoleCreateValidation().validateRequest(request);
        List<Authorization> authorizations = new RoleCreateConstraint(roleRepository, authorizationRepository).checkConstraints(request);

        Role role = new Role(
                UUID.randomUUID(),
                request.name(),
                request.description(),
                false // Always false for user-created roles
        );
        roleRepository.persist(role);

        if (!authorizations.isEmpty()) {
            List<RoleAuthorization> mappings = new ArrayList<>();
            for (Authorization auth : authorizations) {
                mappings.add(new RoleAuthorization(role, auth));
            }
            roleAuthorizationRepository.persistEntriesInBatch(mappings);
            try {
                CacheManager.getInstance().invalidate(RoleAuthorizationKeyCacheConfig.NAME, role.getId());
            } catch (Exception ignored) {}
        }

        return roleAssembler.toCreateResponseDTO(role, authorizations);
    }
}
