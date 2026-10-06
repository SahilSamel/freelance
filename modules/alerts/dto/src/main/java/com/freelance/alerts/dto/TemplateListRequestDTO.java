package com.freelance.alerts.dto;

import com.freelance.alerts.TemplateStatus;

public record TemplateListRequestDTO(
        String category,
        TemplateStatus status,
        Boolean all
) {
    public TemplateListRequestDTO(String category, TemplateStatus status) {
        this(category, status, false);
    }
}
