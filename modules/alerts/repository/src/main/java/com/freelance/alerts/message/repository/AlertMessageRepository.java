package com.freelance.alerts.message.repository;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.rest.app.repository.AbstractRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AlertMessageRepository extends AbstractRepository<AlertMessageEntity, UUID> {

    public List<AlertMessageEntity> findByUserId(UUID userId) {
        if (userId == null) return List.of();
        return executeListResultSearch("AlertMessage.findByUserId", Map.of("userId", userId));
    }

    public List<AlertMessageEntity> findByUserAndChannel(UUID userId, AlertChannel channel) {
        if (userId == null || channel == null) return List.of();
        return executeListResultSearch("AlertMessage.findByUserAndChannel", Map.of(
                "userId", userId,
                "channel", channel
        ));
    }

    public Optional<AlertMessageEntity> findByMetaMessageId(String metaMessageId) {
        if (metaMessageId == null || metaMessageId.isBlank()) return Optional.empty();
        return executeSingleResultSearch("AlertMessage.findByMetaMessageId", Map.of("metaMessageId", metaMessageId.trim()));
    }

    public Optional<AlertMessageEntity> findLatestByUserId(UUID userId) {
        if (userId == null) return Optional.empty();
        return executeListResultSearch("AlertMessage.findLatestByUserId", Map.of("userId", userId))
                .stream()
                .findFirst();
    }

    public Optional<AlertMessageEntity> findLatestByUserAndChannel(UUID userId, AlertChannel channel) {
        if (userId == null || channel == null) return Optional.empty();
        return executeListResultSearch("AlertMessage.findLatestByUserAndChannel", Map.of(
                "userId", userId,
                "channel", channel
        )).stream().findFirst();
    }

    public List<AlertMessageEntity> findUnreadByUserAndChannel(UUID userId, AlertChannel channel) {
        if (userId == null || channel == null) return List.of();
        return executeListResultSearch("AlertMessage.findUnreadByUserAndChannel", Map.of(
                "userId", userId,
                "channel", channel
        ));
    }

    public List<AlertMessageEntity> findUnreadByUserId(UUID userId) {
        if (userId == null) return List.of();
        return executeListResultSearch("AlertMessage.findUnreadByUserId", Map.of("userId", userId));
    }

    public List<AlertMessageEntity> findAllByChannel(AlertChannel channel) {
        if (channel == null) return List.of();
        return em().createQuery(
                "SELECT m FROM AlertMessageEntity m WHERE m.channel = :channel ORDER BY m.timestamp ASC, m.createdDate ASC",
                AlertMessageEntity.class
        ).setParameter("channel", channel).getResultList();
    }
}