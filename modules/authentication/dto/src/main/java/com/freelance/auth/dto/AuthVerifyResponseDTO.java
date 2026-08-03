package com.freelance.auth.dto;

import java.util.UUID;

public record AuthVerifyResponseDTO(UUID userId, String sessionKey) {
}
