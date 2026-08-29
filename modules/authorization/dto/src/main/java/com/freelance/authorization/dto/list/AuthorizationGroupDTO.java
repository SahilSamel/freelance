package com.freelance.authorization.dto.list;

import com.freelance.authorization.dto.AuthorizationDTO;
import java.util.List;

public record AuthorizationGroupDTO(
        String name,
        List<AuthorizationGroupDTO> subGroups,
        List<AuthorizationDTO> authorizations
) {}