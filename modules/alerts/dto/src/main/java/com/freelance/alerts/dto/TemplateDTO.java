package com.freelance.alerts.dto;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.TemplateStatus;
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
        List<TemplateButtonDTO> buttons
) {}
