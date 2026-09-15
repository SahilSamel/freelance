package com.freelance.template.seeder;

import com.freelance.alerts.template.entity.AlertTemplateCategoryEntity;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TemplateCategorySeeder {

    private static final Logger logger = LoggerFactory.getLogger(TemplateCategorySeeder.class);
    private final AlertTemplateCategoryRepository categoryRepository;

    public record DefaultCategory(String key, String name, String description, String priority) {}

    private static final List<DefaultCategory> DEFAULT_CATEGORIES = List.of(
            new DefaultCategory(
                    "AUTHENTICATION",
                    "Authentication",
                    "One-time passwords, 2FA verification codes, and security alerts",
                    "HIGH"
            ),
            new DefaultCategory(
                    "UTILITY",
                    "Utility",
                    "Transactional updates, order confirmations, billing receipts, and account notifications",
                    "NORMAL"
            ),
            new DefaultCategory(
                    "MARKETING",
                    "Marketing",
                    "Promotional campaigns, offers, product announcements, and welcome messages",
                    "LOW"
            )
    );

    public TemplateCategorySeeder() {
        this(new AlertTemplateCategoryRepository());
    }

    public TemplateCategorySeeder(AlertTemplateCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void seed() {
        logger.info("[TemplateCategorySeeder] Checking template categories...");
        for (DefaultCategory def : DEFAULT_CATEGORIES) {
            Optional<AlertTemplateCategoryEntity> existing = categoryRepository.findByKey(def.key());
            if (existing.isEmpty()) {
                logger.info("[TemplateCategorySeeder] Seeding missing template category '{}'", def.key());
                AlertTemplateCategoryEntity entity = new AlertTemplateCategoryEntity(
                        UUID.randomUUID(),
                        def.key(),
                        def.name(),
                        def.description(),
                        def.priority()
                );
                categoryRepository.persist(entity);
            }
        }
        logger.info("[TemplateCategorySeeder] Template category check completed.");
    }
}
