package com.freelance.configuration.dto.system.get;

import java.util.List;

public record SystemConfigurationGetRequestDTO(
        List<String> keys
) {
    public static SystemConfigurationGetRequestDTO of(String... keys) {
        return new SystemConfigurationGetRequestDTO(List.of(keys));
    }
}
