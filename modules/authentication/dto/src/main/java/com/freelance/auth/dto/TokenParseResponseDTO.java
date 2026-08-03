package com.freelance.auth.dto;

import java.util.UUID;

public record TokenParseResponseDTO(UUID userId, String sessionKey) {
}
