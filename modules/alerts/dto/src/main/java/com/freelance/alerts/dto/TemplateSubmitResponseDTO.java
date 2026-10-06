package com.freelance.alerts.dto;

import com.freelance.alerts.TemplateStatus;
import java.util.UUID;

public record TemplateSubmitResponseDTO(
        UUID id,
        String templateKey,
        boolean successful,
        TemplateStatus status,
        String metaTemplateId,
        String errorMessage
) {}
