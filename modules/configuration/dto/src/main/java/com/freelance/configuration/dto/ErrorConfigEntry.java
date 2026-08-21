package com.freelance.configuration.dto;

import java.io.Serializable;

/**
 * Cached entry containing dynamic HTTP status code and user-facing keys message.
 */
public record ErrorConfigEntry(
        int statusCode,
        String errorMessage
) implements Serializable {
}
