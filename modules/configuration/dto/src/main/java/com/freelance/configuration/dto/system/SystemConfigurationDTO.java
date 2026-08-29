package com.freelance.configuration.dto.system;

import java.time.LocalDateTime;

public record SystemConfigurationDTO(
        String configId,
        String configValue,
        String description,
        LocalDateTime createdDate,
        LocalDateTime lastUpdatedDate
) {}
