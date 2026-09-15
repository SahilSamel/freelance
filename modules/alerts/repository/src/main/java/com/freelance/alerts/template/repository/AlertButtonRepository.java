package com.freelance.alerts.template.repository;

import com.freelance.alerts.template.entity.AlertButtonEntity;
import com.freelance.rest.app.repository.AbstractRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AlertButtonRepository extends AbstractRepository<AlertButtonEntity, UUID> {

    public Optional<AlertButtonEntity> findByPayload(String payload) {
        if (payload == null || payload.isBlank()) return Optional.empty();
        return executeSingleResultSearch("AlertButton.findByPayload", Map.of("payload", payload));
    }
}