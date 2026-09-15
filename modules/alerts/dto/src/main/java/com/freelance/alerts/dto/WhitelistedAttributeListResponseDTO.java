package com.freelance.alerts.dto;

import java.util.List;

public record WhitelistedAttributeListResponseDTO(
        List<WhitelistedAttributeDTO> attributes
) {}
