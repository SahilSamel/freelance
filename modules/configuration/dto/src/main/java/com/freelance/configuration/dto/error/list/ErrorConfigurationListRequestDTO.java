package com.freelance.configuration.dto.error.list;

public record ErrorConfigurationListRequestDTO(
        String prefix,
        Integer statusCode
) {
    public static ErrorConfigurationListRequestDTO empty() {
        return new ErrorConfigurationListRequestDTO(null, null);
    }
}
