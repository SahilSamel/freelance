package com.freelance.alerts.dto;

import com.freelance.alerts.AlertChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public record TemplateCreateRequestDTO(
        String templateKey,
        String name,
        String category,
        String priority,
        String language,
        String content,
        String mediaUrl,
        String mediaType,
        AlertChannel channel,
        Set<String> requiredAttributes,
        List<TemplateButtonDTO> buttons,
        List<TemplateCreateRequestDTO> templates
) {
    public TemplateCreateRequestDTO(
            String templateKey,
            String name,
            String category,
            String language,
            String content,
            String mediaUrl,
            String mediaType,
            AlertChannel channel,
            Set<String> requiredAttributes,
            List<TemplateButtonDTO> buttons
    ) {
        this(templateKey, name, category, null, language, content, mediaUrl, mediaType, channel, requiredAttributes, buttons, Collections.emptyList());
    }

    public TemplateCreateRequestDTO(
            String templateKey,
            String name,
            String category,
            String language,
            String content,
            String mediaUrl,
            String mediaType,
            AlertChannel channel,
            Set<String> requiredAttributes,
            List<TemplateButtonDTO> buttons,
            List<TemplateCreateRequestDTO> templates
    ) {
        this(templateKey, name, category, null, language, content, mediaUrl, mediaType, channel, requiredAttributes, buttons, templates);
    }

    /**
     * Collects all template definitions provided in this request, including the root template
     * and any nested/additional templates.
     */
    public List<TemplateCreateRequestDTO> allTemplates() {
        List<TemplateCreateRequestDTO> list = new ArrayList<>();
        if (this.templates != null && !this.templates.isEmpty()) {
            if (this.templateKey != null && !this.templateKey.isBlank()) {
                list.add(this);
            }
            for (TemplateCreateRequestDTO sub : this.templates) {
                if (sub != null) {
                    list.addAll(sub.allTemplates());
                }
            }
        } else {
            list.add(this);
        }
        return list;
    }
}
