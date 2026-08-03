package com.freelance.auth.dto;

import java.util.UUID;

public record SessionInvalidateRequestDTO(String sessionKey, UUID userId) {
}
