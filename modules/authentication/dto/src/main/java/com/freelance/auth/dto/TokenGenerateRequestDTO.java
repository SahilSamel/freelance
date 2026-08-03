package com.freelance.auth.dto;

import java.util.UUID;

public record TokenGenerateRequestDTO(UUID userId, String sessionKey) {
}
