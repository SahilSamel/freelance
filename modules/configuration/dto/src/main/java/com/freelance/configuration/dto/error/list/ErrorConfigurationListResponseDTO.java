package com.freelance.configuration.dto.error.list;

import java.util.List;

public record ErrorConfigurationListResponseDTO(
        List<ErrorConfigurationGroupDTO> groups,
        int totalCount
) {}