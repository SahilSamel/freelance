package com.freelance.user.create.assembler;

import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.user.contact.UserContactResponseDTO;
import com.freelance.user.entity.User;
import com.freelance.user.create.UserCreateRequestDTO;
import com.freelance.user.create.UserCreateResponseDTO;
import com.freelance.utility.password.HashUtility;

import java.util.List;
import java.util.UUID;

public class UserCreateAssembler {

    public User toEntity(UserCreateRequestDTO request) {
        if (request == null) return null;
        String hashedPassword = HashUtility.hash(request.password());
        User user = new User(request.username(), hashedPassword, request.firstName(), request.lastName());
        if (request.contacts() != null) {
            for (var contactDto : request.contacts()) {
                if (contactDto != null && contactDto.channel() != null && contactDto.contactValue() != null && !contactDto.contactValue().isBlank()) {
                    UserContactEntity contactEntity = new UserContactEntity(
                            UUID.randomUUID(),
                            user.getId(),
                            contactDto.channel(),
                            contactDto.contactValue().trim(),
                            false,
                            true,
                            null
                    );
                    user.addContact(contactEntity);
                }
            }
        }
        return user;
    }

    public UserCreateResponseDTO fromEntity(User user) {
        if (user == null) return null;
        return fromEntity(user, List.of());
    }

    public UserCreateResponseDTO fromEntity(User user, List<UUID> roleIds) {
        if (user == null) return null;
        List<UserContactResponseDTO> contacts = user.getContacts() != null
                ? user.getContacts().stream().map(this::toContactResponseDTO).toList()
                : List.of();
        return new UserCreateResponseDTO(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), roleIds != null ? roleIds : List.of(), contacts);
    }

    public UserCreateResponseDTO fromEntity(User user, List<UUID> roleIds, List<UserContactResponseDTO> contacts) {
        if (user == null) return null;
        return new UserCreateResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                roleIds != null ? roleIds : List.of(),
                contacts != null ? contacts : List.of()
        );
    }

    public UserContactResponseDTO toContactResponseDTO(UserContactEntity entity) {
        if (entity == null) return null;
        return new UserContactResponseDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getChannel(),
                entity.getContactValue(),
                entity.isOptedOut(),
                entity.isActive()
        );
    }
}
