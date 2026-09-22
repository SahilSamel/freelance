package com.freelance.user.create;

import com.freelance.alerts.AlertChannel;
import com.freelance.user.contact.UserContactRequestDTO;

import java.util.List;
import java.util.UUID;

public record UserCreateRequestDTO(
        String username,
        String password,
        List<UUID> roleIds,
        List<UserContactRequestDTO> contacts
) {
    public UserCreateRequestDTO(String username, String password) {
        this(username, password, List.of(), List.of());
    }

    public UserCreateRequestDTO(String username, String password, List<UUID> roleIds) {
        this(username, password, roleIds != null ? roleIds : List.of(), List.of());
    }

    public UserCreateRequestDTO(String username, String password, List<UUID> roleIds, List<UserContactRequestDTO> contacts) {
        this.username = username;
        this.password = password;
        this.roleIds = roleIds != null ? roleIds : List.of();
        this.contacts = contacts != null ? contacts : List.of();
    }

    public UserCreateRequestDTO(String username, String password, AlertChannel channel, String contactValue) {
        this(username, password, List.of(), List.of(new UserContactRequestDTO(channel, contactValue)));
    }

    public UserCreateRequestDTO(String username, String password, List<UUID> roleIds, AlertChannel channel, String contactValue) {
        this(username, password, roleIds, List.of(new UserContactRequestDTO(channel, contactValue)));
    }
}
