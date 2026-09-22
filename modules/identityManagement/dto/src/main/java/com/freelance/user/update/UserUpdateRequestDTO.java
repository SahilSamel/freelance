package com.freelance.user.update;

import com.freelance.alerts.AlertChannel;
import com.freelance.user.contact.UserContactRequestDTO;

import java.util.List;
import java.util.UUID;

public record UserUpdateRequestDTO(
        UUID id,
        String username,
        String password,
        List<UserContactRequestDTO> contacts
) {
    public UserUpdateRequestDTO(UUID id, String username, String password) {
        this(id, username, password, null);
    }

    public UserUpdateRequestDTO(UUID id, String username, String password, AlertChannel channel, String contactValue) {
        this(id, username, password, List.of(new UserContactRequestDTO(channel, contactValue)));
    }
}
