package com.freelance.alerts.service.seeder;

import com.freelance.alerts.template.entity.AlertTemplateCategoryEntity;
import com.freelance.alerts.template.repository.AlertTemplateCategoryRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.template.seeder.TemplateCategorySeeder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCategorySeederTest {

    static class InMemoryCategoryRepository extends AlertTemplateCategoryRepository {
        final Map<String, AlertTemplateCategoryEntity> storage = new HashMap<>();

        @Override
        public Optional<AlertTemplateCategoryEntity> findByKey(String key) {
            if (key == null) return Optional.empty();
            return Optional.ofNullable(storage.get(key.toUpperCase()));
        }

        @Override
        public List<AlertTemplateCategoryEntity> findAllCategories() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void persist(AlertTemplateCategoryEntity entity) {
            storage.put(entity.getCategoryKey().toUpperCase(), entity);
        }
    }

    private TransactionContext context;
    private InMemoryCategoryRepository repo;
    private TemplateCategorySeeder seeder;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        repo = new InMemoryCategoryRepository();
        seeder = new TemplateCategorySeeder(repo);
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("Should seed all default categories when repository is empty")
    void testSeedEmptyRepository() {
        assertEquals(0, repo.findAllCategories().size());

        seeder.seed();

        assertEquals(3, repo.findAllCategories().size());
        assertTrue(repo.findByKey("AUTHENTICATION").isPresent());
        assertTrue(repo.findByKey("UTILITY").isPresent());
        assertTrue(repo.findByKey("MARKETING").isPresent());

        assertEquals("HIGH", repo.findByKey("AUTHENTICATION").get().getPriority());
        assertEquals("NORMAL", repo.findByKey("UTILITY").get().getPriority());
        assertEquals("LOW", repo.findByKey("MARKETING").get().getPriority());
    }

    @Test
    @DisplayName("Should be idempotent and not duplicate existing categories")
    void testSeedIdempotent() {
        seeder.seed();
        assertEquals(3, repo.findAllCategories().size());

        seeder.seed();
        assertEquals(3, repo.findAllCategories().size());
    }

    @Test
    @DisplayName("Should preserve pre-existing categories and only seed missing ones")
    void testPreservesExistingCategory() {
        AlertTemplateCategoryEntity customAuth = new AlertTemplateCategoryEntity(
                UUID.randomUUID(), "AUTHENTICATION", "Custom Auth", "Custom desc", "HIGH"
        );
        repo.persist(customAuth);

        seeder.seed();

        assertEquals(3, repo.findAllCategories().size());
        assertEquals("Custom Auth", repo.findByKey("AUTHENTICATION").get().getName());
    }
}
