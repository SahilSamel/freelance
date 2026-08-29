package com.freelance.authorization.dto.list;

import java.util.List;

public record AuthorizationListResponseDTO(
        List<AuthorizationGroupDTO> groups,
        int totalCount
) {}