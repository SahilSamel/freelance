package com.freelance.alerts.dto;

import com.freelance.alerts.TemplateStatus;

public record TemplateListRequestDTO(
        String category,
        TemplateStatus status
) {}
