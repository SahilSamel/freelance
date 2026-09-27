package com.freelance.alerts.preference.repository;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.preference.entity.UserContactEntity;
import com.freelance.rest.app.repository.AbstractRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserContactRepository extends AbstractRepository<UserContactEntity, UUID> {

    public Optional<UserContactEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
        if (userId == null || channel == null) return Optional.empty();
        return executeSingleResultSearch("UserContact.findByUserAndChannel", Map.of(
                "userId", userId,
                "channel", channel
        ));
    }

    public Optional<UserContactEntity> findByContactAndChannel(String contactValue, AlertChannel channel) {
        if (contactValue == null || channel == null) return Optional.empty();
        Optional<UserContactEntity> direct = executeSingleResultSearch("UserContact.findByContactAndChannel", Map.of(
                "contactValue", contactValue.trim(),
                "channel", channel
        ));
        if (direct.isPresent()) {
            return direct;
        }
        if (channel == AlertChannel.WHATSAPP) {
            String trimmed = contactValue.trim();
            String alt = trimmed.startsWith("+") ? trimmed.substring(1) : "+" + trimmed;
            return executeSingleResultSearch("UserContact.findByContactAndChannel", Map.of(
                    "contactValue", alt,
                    "channel", channel
            ));
        }
        return Optional.empty();
    }

    public java.util.List<UserContactEntity> findByUserId(UUID userId) {
        if (userId == null) return java.util.Collections.emptyList();
        return executeListResultSearch("UserContact.findByUserId", Map.of(
                "userId", userId
        ));
    }

    public java.util.List<UserContactEntity> findByUserIds(java.util.List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return java.util.Collections.emptyList();
        return em().createQuery("SELECT c FROM UserContactEntity c WHERE c.userId IN :userIds", UserContactEntity.class)
                .setParameter("userIds", userIds)
                .getResultList();
    }

    public java.util.List<UserContactEntity> findAllContacts() {
        return em().createQuery("SELECT c FROM UserContactEntity c", UserContactEntity.class).getResultList();
    }
}