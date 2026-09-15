package com.freelance.fixtures;

import com.freelance.alerts.template.entity.AlertTemplateCategoryEntity;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;

import java.util.*;

public class FakeAlertTemplateCategoryRepository extends AlertTemplateCategoryRepository {
    private final Map<String, AlertTemplateCategoryEntity> storage = new HashMap<>();

    public FakeAlertTemplateCategoryRepository() {
        seedDefaults();
    }

    public void seedDefaults() {
        save(new AlertTemplateCategoryEntity(UUID.randomUUID(), "AUTHENTICATION", "Authentication", "desc", "HIGH"));
        save(new AlertTemplateCategoryEntity(UUID.randomUUID(), "UTILITY", "Utility", "desc", "NORMAL"));
        save(new AlertTemplateCategoryEntity(UUID.randomUUID(), "MARKETING", "Marketing", "desc", "LOW"));
    }

    public void save(AlertTemplateCategoryEntity entity) {
        storage.put(entity.getCategoryKey().toUpperCase(), entity);
    }

    @Override
    public Optional<AlertTemplateCategoryEntity> findByKey(String categoryKey) {
        if (categoryKey == null || categoryKey.isBlank()) return Optional.empty();
        return Optional.ofNullable(storage.get(categoryKey.trim().toUpperCase()));
    }

    @Override
    public List<AlertTemplateCategoryEntity> findAllCategories() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public void persist(AlertTemplateCategoryEntity entity) {
        save(entity);
    }
}
