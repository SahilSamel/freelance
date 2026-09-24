package com.freelance.auth.dto;

import java.util.UUID;

public record TokenGenerateRequestDTO(UUID userId, String sessionKey, String tenantId) {

    public TokenGenerateRequestDTO(UUID userId, String sessionKey) {
        this(userId, sessionKey, null);
    }
}
