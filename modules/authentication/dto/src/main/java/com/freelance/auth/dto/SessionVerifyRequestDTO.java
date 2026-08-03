package com.freelance.auth.dto;

import java.util.UUID;

public record SessionVerifyRequestDTO(String sessionKey, UUID userId) {
}
