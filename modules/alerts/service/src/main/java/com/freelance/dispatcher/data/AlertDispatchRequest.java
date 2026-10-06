package com.freelance.dispatcher.data;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.event.model.EventPriority;

import java.util.Map;
import java.util.UUID;

public record AlertDispatchRequest(
        UUID userId,
        AlertTemplateEntity template,
        AlertChannel channel,
        String recipientContact,
        String renderedContent,
        Map<String, String> resolvedAttributes,
        EventPriority priority,
        String mediaUrl,
        String mediaId,
        String mediaType
) {
    public AlertDispatchRequest(
            UUID userId,
            AlertTemplateEntity template,
            AlertChannel channel,
            String recipientContact,
            String renderedContent,
            Map<String, String> resolvedAttributes,
            EventPriority priority
    ) {
        this(userId, template, channel, recipientContact, renderedContent, resolvedAttributes, priority, null, null, null);
    }
}
