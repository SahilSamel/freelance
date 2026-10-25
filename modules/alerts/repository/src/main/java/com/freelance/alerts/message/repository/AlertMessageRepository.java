package com.freelance.alerts.message.repository;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.AlertDirection;
import com.freelance.alerts.AlertMessageStatus;
import com.freelance.alerts.message.entity.AlertMessageEntity;
import com.freelance.rest.app.repository.AbstractRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    public List<AlertMessageEntity> findRecentByUserAndChannel(UUID userId, AlertChannel channel, Integer limit, Instant beforeTimestamp) {
        if (userId == null || channel == null) return List.of();
        int max = (limit != null && limit > 0) ? limit : 50;

        List<AlertMessageEntity> list;
        if (beforeTimestamp != null) {
            list = em().createQuery(
                    "SELECT m FROM AlertMessageEntity m WHERE m.userId = :userId AND m.channel = :channel AND m.timestamp < :beforeTimestamp ORDER BY m.timestamp DESC, m.createdDate DESC",
                    AlertMessageEntity.class
            )
            .setParameter("userId", userId)
            .setParameter("channel", channel)
            .setParameter("beforeTimestamp", beforeTimestamp)
            .setMaxResults(max)
            .getResultList();
        } else {
            list = em().createQuery(
                    "SELECT m FROM AlertMessageEntity m WHERE m.userId = :userId AND m.channel = :channel ORDER BY m.timestamp DESC, m.createdDate DESC",
                    AlertMessageEntity.class
            )
            .setParameter("userId", userId)
            .setParameter("channel", channel)
            .setMaxResults(max)
            .getResultList();
        }

        if (list != null && list.size() > 1) {
            java.util.Collections.reverse(list);
        }
        return list != null ? list : List.of();
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

    public int markAllInboundAsRead(UUID userId, AlertChannel channel) {
        if (userId == null || channel == null) return 0;
        return em().createQuery(
                "UPDATE AlertMessageEntity m SET m.status = :readStatus, m.lastUpdatedDate = :now WHERE m.userId = :userId AND m.channel = :channel AND m.direction = :inboundDir AND m.status != :readStatus"
        )
        .setParameter("readStatus", AlertMessageStatus.READ)
        .setParameter("now", LocalDateTime.now())
        .setParameter("userId", userId)
        .setParameter("channel", channel)
        .setParameter("inboundDir", AlertDirection.INBOUND)
        .executeUpdate();
    }
}