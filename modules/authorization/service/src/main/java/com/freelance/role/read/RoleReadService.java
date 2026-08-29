package com.freelance.role.read;

import com.freelance.authorization.dto.role.RoleDTO;
import com.freelance.authorization.dto.role.list.RoleListRequestDTO;
import com.freelance.authorization.dto.role.list.RoleListResponseDTO;
import com.freelance.authorization.dto.role.read.RoleReadRequestDTO;
import com.freelance.authorization.dto.role.read.RoleReadResponseDTO;
import com.freelance.entity.authorization.Authorization;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorization;
import com.freelance.entity.role.Role;
import com.freelance.repository.mapping.roleAuthorization.RoleAuthorizationRepository;
import com.freelance.repository.role.RoleRepository;
import com.freelance.role.assembler.RoleAssembler;
import com.freelance.role.read.constraint.RoleReadConstraint;
import com.freelance.role.read.validation.RoleReadValidation;

import java.util.*;

public class RoleReadService {

    private final RoleRepository roleRepository;
    private final RoleAuthorizationRepository roleAuthorizationRepository;
    private final RoleAssembler roleAssembler;

    public RoleReadService() {
        this(new RoleRepository(), new RoleAuthorizationRepository(), new RoleAssembler());
    }

    public RoleReadService(
            RoleRepository roleRepository,
            RoleAuthorizationRepository roleAuthorizationRepository,
            RoleAssembler roleAssembler
    ) {
        this.roleRepository = roleRepository;
        this.roleAuthorizationRepository = roleAuthorizationRepository;
        this.roleAssembler = roleAssembler;
    }

    public RoleReadResponseDTO readRole(RoleReadRequestDTO request) {
        new RoleReadValidation().validateRequest(request);

        Role role = null;
        if (request.id() != null) {
            role = roleRepository.findById(request.id()).orElse(null);
        } else if (request.name() != null && !request.name().isBlank()) {
            role = roleRepository.findByName(request.name());
        }

        new RoleReadConstraint().checkRoleExists(role);

        List<RoleAuthorization> mappings = roleAuthorizationRepository.getRoleAuthorizationsByRoleId(role.getId());
        List<Authorization> authorizations = mappings.stream()
                .map(RoleAuthorization::getAuthorization)
                .filter(Objects::nonNull)
                .toList();

        return new RoleReadResponseDTO(roleAssembler.toDTO(role, authorizations));
    }

    public RoleListResponseDTO listRoles(RoleListRequestDTO request) {
        List<Role> roles = roleRepository.findAll();
        if (request != null && Boolean.TRUE.equals(request.systemOnly())) {
            roles = roles.stream().filter(Role::isSystem).toList();
        }

        Map<UUID, List<RoleAuthorization>> mappingsByRole = roleAuthorizationRepository.getAllMappingsGroupedByRoleId();

        List<RoleDTO> roleDTOs = roles.stream()
                .map(role -> {
                    List<RoleAuthorization> mappings = mappingsByRole.getOrDefault(role.getId(), Collections.emptyList());
                    List<Authorization> authorizations = mappings.stream()
                            .map(RoleAuthorization::getAuthorization)
                            .filter(Objects::nonNull)
                            .toList();
                    return roleAssembler.toDTO(role, authorizations);
                })
                .toList();

        return new RoleListResponseDTO(roleDTOs);
    }
}
