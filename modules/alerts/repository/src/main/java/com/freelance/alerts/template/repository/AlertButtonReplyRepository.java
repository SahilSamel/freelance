package com.freelance.alerts.template.repository;

import com.freelance.alerts.template.entity.AlertButtonReplyEntity;
import com.freelance.rest.app.repository.AbstractRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AlertButtonReplyRepository extends AbstractRepository<AlertButtonReplyEntity, UUID> {

    public Optional<AlertButtonReplyEntity> findByButtonPayload(String payload) {
        if (payload == null || payload.isBlank()) return Optional.empty();
        return executeSingleResultSearch("AlertButtonReply.findByButtonPayload", Map.of("payload", payload));
    }
}