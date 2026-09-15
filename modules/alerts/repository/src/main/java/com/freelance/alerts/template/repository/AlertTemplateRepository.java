package com.freelance.alerts.template.repository;

import com.freelance.alerts.TemplateStatus;
import com.freelance.alerts.template.entity.AlertTemplateEntity;
import com.freelance.rest.app.repository.AbstractRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AlertTemplateRepository extends AbstractRepository<AlertTemplateEntity, UUID> {

    public Optional<AlertTemplateEntity> findByKey(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) return Optional.empty();
        return executeSingleResultSearch("AlertTemplate.findByKey", Map.of("templateKey", templateKey.trim()));
    }

    public List<AlertTemplateEntity> findAllTemplates() {
        return executeListResultSearch("AlertTemplate.findAll", null);
    }

    public List<AlertTemplateEntity> findByStatus(TemplateStatus status) {
        if (status == null) return findAllTemplates();
        return executeListResultSearch("AlertTemplate.findByStatus", Map.of("status", status));
    }

    public Optional<AlertTemplateEntity> findByKeyIgnoreCase(String templateKey) {
        if (templateKey == null || templateKey.isBlank()) return Optional.empty();
        return executeSingleResultSearch("AlertTemplate.findByKeyIgnoreCase", Map.of("templateKey", templateKey.trim()));
    }

    public Optional<AlertTemplateEntity> findByMetaTemplateId(String metaTemplateId) {
        if (metaTemplateId == null || metaTemplateId.isBlank()) return Optional.empty();
        return executeSingleResultSearch("AlertTemplate.findByMetaTemplateId", Map.of("metaTemplateId", metaTemplateId.trim()));
    }

    public void flush() {
        em().flush();
    }
}