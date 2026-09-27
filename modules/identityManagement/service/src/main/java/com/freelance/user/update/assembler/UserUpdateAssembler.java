package com.freelance.user.update.assembler;

import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.contact.UserContactResponseDTO;
import com.freelance.user.entity.User;
import com.freelance.user.update.UserUpdateRequestDTO;
import com.freelance.user.update.UserUpdateResponseDTO;
import com.freelance.utility.password.HashUtility;

import java.util.List;
import java.util.UUID;

public class UserUpdateAssembler {

    public void applyUpdates(User user, UserUpdateRequestDTO request) {
        if (user == null || request == null) return;

        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(HashUtility.hash(request.password()));
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }

        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        if (request.contacts() != null) {
            java.util.Map<com.freelance.alerts.AlertChannel, UserContactEntity> existingByChannel = new java.util.HashMap<>();
            for (UserContactEntity c : user.getContacts()) {
                existingByChannel.put(c.getChannel(), c);
            }

            List<UserContactEntity> updatedList = new java.util.ArrayList<>();
            for (UserContactRequestDTO contactDto : request.contacts()) {
                if (contactDto != null && contactDto.channel() != null && contactDto.contactValue() != null && !contactDto.contactValue().isBlank()) {
                    UserContactEntity existing = existingByChannel.remove(contactDto.channel());
                    if (existing != null) {
                        existing.setContactValue(contactDto.contactValue().trim());
                        updatedList.add(existing);
                    } else {
                        updatedList.add(new UserContactEntity(
                                UUID.randomUUID(),
                                user.getId(),
                                contactDto.channel(),
                                contactDto.contactValue().trim(),
                                false,
                                true,
                                null
                        ));
                    }
                }
            }
            user.getContacts().clear();
            user.getContacts().addAll(updatedList);
        }
    }

    public UserUpdateResponseDTO fromEntity(User user) {
        if (user == null) return null;
        List<UserContactResponseDTO> contacts = user.getContacts() != null
                ? user.getContacts().stream().map(this::toContactResponseDTO).toList()
                : List.of();
        return new UserUpdateResponseDTO(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), contacts);
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
