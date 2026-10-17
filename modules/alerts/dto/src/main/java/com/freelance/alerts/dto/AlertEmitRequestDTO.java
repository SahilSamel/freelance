package com.freelance.alerts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freelance.event.model.EventPriority;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertEmitRequestDTO(
        UUID templateId,
        List<UUID> userIds,
        String rawContent,
        Map<String, String> customParameters
) {
    public AlertEmitRequestDTO(UUID templateId, List<UUID> userIds) {
        this(templateId, userIds, null, null);
    }

    public AlertEmitRequestDTO(UUID templateId, List<UUID> userIds, String rawContent) {
        this(templateId, userIds, rawContent, null);
    }

    /**
     * Backward-compatible constructor that ignores the priority parameter.
     * Priority is automatically derived from the alert template's category.
     */
    @Deprecated
    public AlertEmitRequestDTO(UUID templateId, List<UUID> userIds, EventPriority priority) {
        this(templateId, userIds, null, null);
    }
}
