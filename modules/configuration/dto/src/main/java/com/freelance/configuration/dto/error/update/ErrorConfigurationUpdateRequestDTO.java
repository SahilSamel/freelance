package com.freelance.configuration.dto.error.update;

public record ErrorConfigurationUpdateRequestDTO(
        String errorId,
        Integer statusCode,
        String errorMessage,
        String description
) {}
