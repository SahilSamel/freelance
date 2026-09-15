package com.freelance.alerts.template.repository;

import com.freelance.alerts.template.entity.AlertTemplateCategoryEntity;
import com.freelance.rest.app.repository.AbstractRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AlertTemplateCategoryRepository extends AbstractRepository<AlertTemplateCategoryEntity, UUID> {

    public Optional<AlertTemplateCategoryEntity> findByKey(String categoryKey) {
        if (categoryKey == null || categoryKey.isBlank()) return Optional.empty();
        return executeSingleResultSearch("AlertTemplateCategory.findByKey", Map.of("categoryKey", categoryKey.trim()));
    }

    public List<AlertTemplateCategoryEntity> findAllCategories() {
        return executeListResultSearch("AlertTemplateCategory.findAll", null);
    }
}
