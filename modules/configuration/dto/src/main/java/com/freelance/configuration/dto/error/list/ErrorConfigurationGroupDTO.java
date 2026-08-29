package com.freelance.configuration.dto.error.list;

import com.freelance.configuration.dto.error.ErrorConfigurationDTO;
import java.util.List;

public record ErrorConfigurationGroupDTO(
        String name,
        List<ErrorConfigurationGroupDTO> subGroups,
        List<ErrorConfigurationDTO> errorConfigurations
) {}