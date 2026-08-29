package com.freelance.configuration.dto.system.list;

public record SystemConfigurationListRequestDTO(
        String prefix
) {
    public static SystemConfigurationListRequestDTO empty() {
        return new SystemConfigurationListRequestDTO(null);
    }
}
