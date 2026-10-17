package com.freelance.alerts.dto;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.DispatchScope;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record TemplateDTO(
        UUID id,
        String templateKey,
        String name,
        String category,
        String priority,
        String language,
        String content,
        String mediaUrl,
        String mediaType,
        AlertChannel channel,
        TemplateStatus status,
        boolean isSystem,
        Set<String> requiredAttributes,
        List<TemplateButtonDTO> buttons,
        DispatchScope dispatchScope
) {
    public TemplateDTO(
            UUID id,
            String templateKey,
            String name,
            String category,
            String priority,
            String language,
            String content,
            String mediaUrl,
            String mediaType,
            AlertChannel channel,
            TemplateStatus status,
            boolean isSystem,
            Set<String> requiredAttributes,
            List<TemplateButtonDTO> buttons
    ) {
        this(id, templateKey, name, category, priority, language, content, mediaUrl, mediaType, channel, status, isSystem, requiredAttributes, buttons, DispatchScope.BOTH);
    }
}
