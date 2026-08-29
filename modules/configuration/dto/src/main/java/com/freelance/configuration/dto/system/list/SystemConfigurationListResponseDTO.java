package com.freelance.configuration.dto.system.list;

import java.util.List;

public record SystemConfigurationListResponseDTO(
        List<SystemConfigurationGroupDTO> groups,
        int totalCount
) {}