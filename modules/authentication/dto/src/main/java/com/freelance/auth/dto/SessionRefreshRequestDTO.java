package com.freelance.auth.dto;

import java.util.UUID;

public record SessionRefreshRequestDTO(String sessionKey, UUID userId) {
}
