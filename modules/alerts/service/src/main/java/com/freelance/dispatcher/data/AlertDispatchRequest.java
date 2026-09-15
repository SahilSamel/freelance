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
        EventPriority priority
) {}
