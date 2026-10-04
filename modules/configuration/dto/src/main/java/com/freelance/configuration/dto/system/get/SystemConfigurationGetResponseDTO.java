package com.freelance.configuration.dto.system.get;

import java.util.Map;

public record SystemConfigurationGetResponseDTO(
        Map<String, String> values
) {}
