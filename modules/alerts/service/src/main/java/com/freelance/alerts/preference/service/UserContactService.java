package com.freelance.alerts.preference.service;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.dto.UserContactDTO;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.alerts.preference.repository.UserContactRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserContactService {

    private final UserContactRepository userContactRepository;

    public UserContactService() {
        this(new UserContactRepository());
    }

    public UserContactService(UserContactRepository userContactRepository) {
        this.userContactRepository = userContactRepository;
    }

    public Optional<UserContactDTO> findByUserAndChannel(UUID userId, AlertChannel channel) {
        if (userId == null || channel == null) {
            return Optional.empty();
        }
        return userContactRepository.findByUserAndChannel(userId, channel)
                .map(this::toDTO);
    }

    public Optional<UserContactDTO> findByContactAndChannel(String contactValue, AlertChannel channel) {
        if (contactValue == null || channel == null) {
            return Optional.empty();
        }
        return userContactRepository.findByContactAndChannel(contactValue, channel)
                .map(this::toDTO);
    }

    public List<UserContactDTO> findByUserId(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return userContactRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    public List<UserContactDTO> findByUserIds(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return userContactRepository.findByUserIds(userIds).stream()
                .map(this::toDTO)
                .toList();
    }

    public UserContactDTO toDTO(UserContactEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserContactDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getChannel(),
                entity.getContactValue(),
                entity.isOptedOut(),
                entity.isActive()
        );
    }
}
