package com.freelance.configuration.dto.system.update;

public record SystemConfigurationUpdateRequestDTO(
        String configId,
        String configValue,
        String description
) {}
