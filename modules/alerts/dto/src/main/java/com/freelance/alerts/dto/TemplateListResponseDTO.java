package com.freelance.alerts.dto;

import java.util.List;

public record TemplateListResponseDTO(
        List<TemplateDTO> templates
) {}
