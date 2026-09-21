package com.freelance.template.seeder;

import com.freelance.alerts.TemplateCategory;
import com.freelance.alerts.template.entity.AlertTemplateCategoryEntity;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class TemplateCategorySeeder {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCategorySeeder.class);
    private final AlertTemplateCategoryRepository categoryRepository;

    public TemplateCategorySeeder() {
        this(new AlertTemplateCategoryRepository());
    }

    public TemplateCategorySeeder(AlertTemplateCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void seed() {
        logger.info("[TemplateCategorySeeder] Checking template categories...");
        for (TemplateCategory category : TemplateCategory.values()) {
            Optional<AlertTemplateCategoryEntity> existing = categoryRepository.findByKey(category.name());
            if (existing.isEmpty()) {
                logger.info("[TemplateCategorySeeder] Seeding missing template category '{}'", category.name());
                AlertTemplateCategoryEntity entity = new AlertTemplateCategoryEntity(
                        UUID.randomUUID(),
                        category.name(),
                        category.getDisplayName(),
                        category.getDescription(),
                        category.getDefaultPriority()
                );
                categoryRepository.persist(entity);
            }
        }
        logger.info("[TemplateCategorySeeder] Template category check completed.");
    }
}
