package com.freelance.configuration.dto.system.list;

import com.freelance.configuration.dto.system.SystemConfigurationDTO;
import java.util.List;

public record SystemConfigurationGroupDTO(
        String name,
        List<SystemConfigurationGroupDTO> subGroups,
        List<SystemConfigurationDTO> configurations
) {}