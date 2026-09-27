package com.freelance.user.update;

import com.freelance.alerts.AlertChannel;
import com.freelance.user.contact.UserContactRequestDTO;

import java.util.List;
import java.util.UUID;

public record UserUpdateRequestDTO(
        UUID id,
        String username,
        String password,
        String firstName,
        String lastName,
        List<UserContactRequestDTO> contacts
) {
    public UserUpdateRequestDTO(UUID id, String username, String password) {
        this(id, username, password, null, null, null);
    }

    public UserUpdateRequestDTO(UUID id, String username, String password, List<UserContactRequestDTO> contacts) {
        this(id, username, password, null, null, contacts);
    }

    public UserUpdateRequestDTO(UUID id, String username, String password, AlertChannel channel, String contactValue) {
        this(id, username, password, null, null, List.of(new UserContactRequestDTO(channel, contactValue)));
    }

    public UserUpdateRequestDTO(UUID id, String username, String password, String firstName, String lastName, List<UserContactRequestDTO> contacts) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.contacts = contacts;
    }
}
