package com.freelance.configuration.dto.error;

import java.time.LocalDateTime;

public record ErrorConfigurationDTO(
        String errorId,
        int statusCode,
        String errorMessage,
        String description,
        LocalDateTime createdDate,
        LocalDateTime lastUpdatedDate
) {}
