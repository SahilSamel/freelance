package com.freelance.auth.dto;

public record SupportRequestResponseDTO(
        boolean requested,
        String message
) {
}
