package com.freelance.configuration.dto.system.update;

public record SystemConfigurationUpdateResponseDTO(
        String configId,
        String configValue,
        String description
) {}
