package com.freelance.alerts.dto;

public record TemplateCategoryDTO(
        String categoryKey,
        String name,
        String description,
        String priority
) {}
