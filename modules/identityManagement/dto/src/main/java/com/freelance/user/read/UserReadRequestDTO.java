package com.freelance.user.read;

public record UserReadRequestDTO(
        String userId,
        String username
) {
}