package com.freelance.role.create.constraint;

import com.freelance.authorization.dto.role.create.RoleCreateRequestDTO;
import com.freelance.authorization.entity.Authorization;
import com.freelance.keys.authorization.permission.PermissionConstraintErrors;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.authorization.repository.AuthorizationRepository;
import com.freelance.role.repository.RoleRepository;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoleCreateConstraint extends AbstractConstraint {

    private final RoleRepository roleRepository;
    private final AuthorizationRepository authorizationRepository;

    public RoleCreateConstraint() {
        this(new RoleRepository(), new AuthorizationRepository());
    }

    public RoleCreateConstraint(RoleRepository roleRepository, AuthorizationRepository authorizationRepository) {
        this.roleRepository = roleRepository;
        this.authorizationRepository = authorizationRepository;
    }

    public List<Authorization> checkConstraints(RoleCreateRequestDTO request) {
        if (request == null) {
            return Collections.emptyList();
        }

        if (roleRepository.existsByName(request.name())) {
            raiseConstraintError(RoleConstraintErrors.ROLE_ALREADY_EXISTS);
        }

        List<Authorization> authEntities = new ArrayList<>();
        if (request.authorizationIds() != null && !request.authorizationIds().isEmpty()) {
            Map<UUID, Authorization> authMap = authorizationRepository.findAll().stream()
                    .collect(java.util.stream.Collectors.toMap(Authorization::getId, a -> a));

            for (UUID authId : request.authorizationIds()) {
                if (authId == null || !authMap.containsKey(authId)) {
                    raiseConstraintError(PermissionConstraintErrors.AUTHORIZATION_NOT_FOUND);
                }
                authEntities.add(authMap.get(authId));
            }
        }

        return authEntities;
    }
}
