package com.freelance.configuration.dto.error.update;

public record ErrorConfigurationUpdateResponseDTO(
        String errorId,
        int statusCode,
        String errorMessage,
        String description
) {}
