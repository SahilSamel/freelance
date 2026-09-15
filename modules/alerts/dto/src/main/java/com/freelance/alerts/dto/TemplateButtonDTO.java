package com.freelance.alerts.dto;

import com.freelance.alerts.ButtonType;
import java.io.Serializable;

public record TemplateButtonDTO(
        String text,
        ButtonType type,
        String payload,
        String replyTemplateKey,
        boolean requiresHuman,
        int sequenceOrder
) implements Serializable {}
