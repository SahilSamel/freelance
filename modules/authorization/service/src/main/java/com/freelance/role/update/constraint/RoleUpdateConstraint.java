package com.freelance.role.update.constraint;

import com.freelance.authorization.dto.role.update.RoleUpdateRequestDTO;
import com.freelance.authorization.entity.Authorization;
import com.freelance.role.entity.Role;
import com.freelance.keys.authorization.permission.PermissionConstraintErrors;
import com.freelance.keys.authorization.role.RoleConstraintErrors;
import com.freelance.authorization.repository.AuthorizationRepository;
import com.freelance.role.repository.RoleRepository;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RoleUpdateConstraint extends AbstractConstraint {

    private final RoleRepository roleRepository;
    private final AuthorizationRepository authorizationRepository;

    public RoleUpdateConstraint() {
        this(new RoleRepository(), new AuthorizationRepository());
    }

    public RoleUpdateConstraint(RoleRepository roleRepository, AuthorizationRepository authorizationRepository) {
        this.roleRepository = roleRepository;
        this.authorizationRepository = authorizationRepository;
    }

    public Role checkConstraints(RoleUpdateRequestDTO request, List<Authorization> outAuthorizations) {
        if (request == null || request.id() == null) {
            return null;
        }

        Role existingRole = roleRepository.findById(request.id()).orElse(null);
        if (existingRole == null) {
            raiseConstraintError(RoleConstraintErrors.ROLE_TO_UPDATE_NOT_FOUND);
        }

        if (existingRole.isSystem()) {
            raiseConstraintError(RoleConstraintErrors.SYSTEM_ROLE_CANNOT_BE_MANAGED);
        }

        if (request.name() != null && !request.name().isBlank()) {
            if (!request.name().equals(existingRole.getName())) {
                Role otherRole = roleRepository.findByName(request.name());
                if (otherRole != null && !otherRole.getId().equals(request.id())) {
                    raiseConstraintError(RoleConstraintErrors.ROLE_ALREADY_EXISTS);
                }
            }
        }

        if (request.authorizationIds() != null && outAuthorizations != null && !request.authorizationIds().isEmpty()) {
            Map<UUID, Authorization> authMap = authorizationRepository.findAll().stream()
                    .collect(java.util.stream.Collectors.toMap(Authorization::getId, a -> a));

            for (UUID authId : request.authorizationIds()) {
                if (authId == null || !authMap.containsKey(authId)) {
                    raiseConstraintError(PermissionConstraintErrors.AUTHORIZATION_NOT_FOUND);
                }
                outAuthorizations.add(authMap.get(authId));
            }
        }

        return existingRole;
    }
}
