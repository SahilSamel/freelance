package com.freelance.user.create;

import com.freelance.alerts.AlertChannel;
import com.freelance.user.contact.UserContactRequestDTO;

import java.util.List;
import java.util.UUID;

public record UserCreateRequestDTO(
        String username,
        String password,
        String firstName,
        String lastName,
        List<UUID> roleIds,
        List<UserContactRequestDTO> contacts
) {
    public UserCreateRequestDTO(String username, String password) {
        this(username, password, null, null, List.of(), List.of());
    }

    public UserCreateRequestDTO(String username, String password, List<UUID> roleIds) {
        this(username, password, null, null, roleIds != null ? roleIds : List.of(), List.of());
    }

    public UserCreateRequestDTO(String username, String password, List<UUID> roleIds, List<UserContactRequestDTO> contacts) {
        this(username, password, null, null, roleIds != null ? roleIds : List.of(), contacts != null ? contacts : List.of());
    }

    public UserCreateRequestDTO(String username, String password, AlertChannel channel, String contactValue) {
        this(username, password, null, null, List.of(), List.of(new UserContactRequestDTO(channel, contactValue)));
    }

    public UserCreateRequestDTO(String username, String password, List<UUID> roleIds, AlertChannel channel, String contactValue) {
        this(username, password, null, null, roleIds, List.of(new UserContactRequestDTO(channel, contactValue)));
    }

    public UserCreateRequestDTO(String username, String password, String firstName, String lastName, List<UUID> roleIds, List<UserContactRequestDTO> contacts) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.roleIds = roleIds != null ? roleIds : List.of();
        this.contacts = contacts != null ? contacts : List.of();
    }
}
