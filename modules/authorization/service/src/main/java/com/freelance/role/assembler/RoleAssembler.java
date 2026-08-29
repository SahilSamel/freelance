package com.freelance.role.assembler;

import com.freelance.authorization.dto.AuthorizationDTO;
import com.freelance.authorization.dto.role.RoleDTO;
import com.freelance.authorization.dto.role.create.RoleCreateResponseDTO;
import com.freelance.authorization.dto.role.update.RoleUpdateResponseDTO;
import com.freelance.authorization.service.list.assembler.AuthorizationListAssembler;
import com.freelance.entity.authorization.Authorization;
import com.freelance.entity.role.Role;

import java.util.Collections;
import java.util.List;

public class RoleAssembler {

    private final AuthorizationListAssembler authorizationAssembler;

    public RoleAssembler() {
        this(new AuthorizationListAssembler());
    }

    public RoleAssembler(AuthorizationListAssembler authorizationAssembler) {
        this.authorizationAssembler = authorizationAssembler;
    }

    public RoleDTO toDTO(Role entity, List<Authorization> authorizations) {
        if (entity == null) {
            return null;
        }
        List<AuthorizationDTO> authDTOs = authorizations != null
                ? authorizations.stream().map(authorizationAssembler::toDTO).toList()
                : Collections.emptyList();

        return new RoleDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isSystem(),
                entity.getCreatedDate(),
                entity.getLastUpdatedDate(),
                authDTOs
        );
    }

    public RoleCreateResponseDTO toCreateResponseDTO(Role entity, List<Authorization> authorizations) {
        if (entity == null) {
            return null;
        }
        List<AuthorizationDTO> authDTOs = authorizations != null
                ? authorizations.stream().map(authorizationAssembler::toDTO).toList()
                : Collections.emptyList();

        return new RoleCreateResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isSystem(),
                authDTOs
        );
    }

    public RoleUpdateResponseDTO toUpdateResponseDTO(Role entity, List<Authorization> authorizations) {
        if (entity == null) {
            return null;
        }
        List<AuthorizationDTO> authDTOs = authorizations != null
                ? authorizations.stream().map(authorizationAssembler::toDTO).toList()
                : Collections.emptyList();

        return new RoleUpdateResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.isSystem(),
                authDTOs
        );
    }
}
