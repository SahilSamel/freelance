package com.freelance.alerts.dto;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TemplateUpdateRequestDTO(
        UUID id,
        String templateKey,
        String name,
        String category,
        String priority,
        String language,
        String content,
        String mediaUrl,
        String mediaType,
        Set<String> requiredAttributes,
        List<TemplateButtonDTO> buttons
) {
    public TemplateUpdateRequestDTO(
            UUID id,
            String templateKey,
            String name,
            String category,
            String language,
            String content,
            String mediaUrl,
            String mediaType,
            Set<String> requiredAttributes,
            List<TemplateButtonDTO> buttons
    ) {
        this(id, templateKey, name, category, null, language, content, mediaUrl, mediaType, requiredAttributes, buttons);
    }

    public TemplateUpdateRequestDTO(UUID id, String content) {
        this(id, null, null, null, null, null, content, null, null, null, null);
    }

    public TemplateUpdateRequestDTO(String templateKey, String content) {
        this(null, templateKey, null, null, null, null, content, null, null, null, null);
    }
}
