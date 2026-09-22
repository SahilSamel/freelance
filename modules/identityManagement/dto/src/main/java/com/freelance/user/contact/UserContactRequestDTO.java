package com.freelance.user.contact;

import com.freelance.alerts.AlertChannel;

/**
 * Request DTO representing contact information supplied during user registration.
 */
public record UserContactRequestDTO(
        AlertChannel channel,
        String contactValue
) {
}
