package com.freelance.configuration.dto;

import java.io.Serializable;

public record ErrorConfigEntry(
        int statusCode,
        String errorMessage
) implements Serializable {
}
