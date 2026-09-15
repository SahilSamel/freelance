package com.freelance.alerts.dto;

import java.util.List;

public record TemplateCategoryListResponseDTO(
        List<TemplateCategoryDTO> categories
) {}
