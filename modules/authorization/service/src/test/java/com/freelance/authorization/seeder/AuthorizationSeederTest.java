package com.freelance.authorization.seeder;

import com.freelance.authorization.entity.Authorization;
import com.freelance.authorization.repository.AuthorizationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationSeederTest {

    // ── In-Memory Fakes ───────────────────────────────────────────────────────

    static class FakeAuthorizationRepository extends AuthorizationRepository {
        final Map<String, Authorization> storage = new HashMap<>();
        List<Authorization> lastPersistedBatch = Collections.emptyList();
        int removeCalls = 0;
        int mergeCalls = 0;

        @Override
        public Set<String> findAllKeys() {
            return new HashSet<>(storage.keySet());
        }

        @Override
        public Map<String, Authorization> findAllAsMap() {
            return new HashMap<>(storage);
        }

        @Override
        public Authorization findByKey(String key) {
            return storage.get(key);
        }

        @Override
        public void persistEntriesInBatch(List<Authorization> entities, int... batchSize) {
            this.lastPersistedBatch = new ArrayList<>(entities);
            for (Authorization a : entities) {
                storage.put(a.getKey(), a);
            }
        }

        @Override
        public Authorization merge(Authorization entity) {
            mergeCalls++;
            storage.put(entity.getKey(), entity);
            return entity;
        }

        @Override
        public List<Authorization> mergeEntriesInBatch(List<Authorization> entities, int... batchSize) {
            if (entities == null) return Collections.emptyList();
            for (Authorization a : entities) {
                merge(a);
            }
            return entities;
        }

        @Override
        public void remove(Authorization entity) {
            if (entity != null) {
                removeCalls++;
                storage.remove(entity.getKey());
            }
        }

        @Override
        public void removeEntriesInBatch(List<Authorization> entities, int... batchSize) {
            if (entities != null) {
                for (Authorization a : entities) {
                    remove(a);
                }
            }
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("seed() discovers endpoint @Authorization annotations and batch-persists entities")
    void seed_discoversAndPersistsEntities() {
        FakeAuthorizationRepository fakeAuthRepo = new FakeAuthorizationRepository();

        AuthorizationSeeder seeder = new AuthorizationSeeder(fakeAuthRepo);
        seeder.seed();

        List<com.freelance.rest.app.endpoint.annotation.Authorization> annotations =
                AuthorizationRegistry.getAllAuthorizations();

        assertFalse(annotations.isEmpty(), "Expected @Authorization annotations to exist in codebase");

        // Verify Authorizations persisted
        assertEquals(annotations.size(), fakeAuthRepo.storage.size());
        for (var ann : annotations) {
            Authorization authEntity = fakeAuthRepo.findByKey(ann.key());
            assertNotNull(authEntity, "Authorization should be persisted for key: " + ann.key());
            assertEquals(ann.name(), authEntity.getName());
            assertEquals(ann.group(), authEntity.getGroupPath());
        }
    }

    @Test
    @DisplayName("seed() is idempotent: when no code changes exist, 0 DB modifications occur on second run")
    void seed_isIdempotentAndSkipsUnchangedMappings() {
        FakeAuthorizationRepository fakeAuthRepo = new FakeAuthorizationRepository();

        AuthorizationSeeder seeder = new AuthorizationSeeder(fakeAuthRepo);

        // Run seed first time
        seeder.seed();
        int initialAuthCount = fakeAuthRepo.storage.size();

        // Clear persisted batch trackers
        fakeAuthRepo.lastPersistedBatch = Collections.emptyList();
        fakeAuthRepo.mergeCalls = 0;
        fakeAuthRepo.removeCalls = 0;

        // Run seed second time (0 changes in code)
        seeder.seed();

        // Counts unchanged
        assertEquals(initialAuthCount, fakeAuthRepo.storage.size());

        // Zero mutations executed on second run
        assertTrue(fakeAuthRepo.lastPersistedBatch.isEmpty(), "Expected 0 new authorizations persisted");
        assertEquals(0, fakeAuthRepo.mergeCalls, "Expected 0 merge calls when metadata is unchanged");
        assertEquals(0, fakeAuthRepo.removeCalls, "Expected 0 remove calls when metadata is unchanged");
    }

    @Test
    @DisplayName("seed() detects and updates changed authorization descriptions/names")
    void seed_updatesMetadataWhenChanged() {
        FakeAuthorizationRepository fakeAuthRepo = new FakeAuthorizationRepository();

        AuthorizationSeeder seeder = new AuthorizationSeeder(fakeAuthRepo);
        seeder.seed();

        // Simulate an outdated description in the DB
        var sampleAuth = fakeAuthRepo.storage.values().iterator().next();
        sampleAuth.setDescription("Outdated description from previous version");

        // Re-run seed
        seeder.seed();

        // Verify description was synchronized back to annotation definition
        List<com.freelance.rest.app.endpoint.annotation.Authorization> annotations =
                AuthorizationRegistry.getAllAuthorizations();
        var matchingAnnotation = annotations.stream()
                .filter(a -> a.key().equals(sampleAuth.getKey()))
                .findFirst()
                .orElseThrow();

        assertEquals(matchingAnnotation.description(), sampleAuth.getDescription());
        assertTrue(fakeAuthRepo.mergeCalls > 0, "Expected merge to be called for updated metadata");
    }

    @Test
    @DisplayName("seed() deletes stale authorizations that were removed from code")
    void seed_deletesStaleEntriesNoLongerInCode() {
        FakeAuthorizationRepository fakeAuthRepo = new FakeAuthorizationRepository();

        // Inject stale authorization into storage
        Authorization staleAuth = new Authorization(
                UUID.randomUUID(), "OBSOLETE_ENDPOINT", "Obsolete", "No longer used", "Obsolete"
        );
        fakeAuthRepo.storage.put(staleAuth.getKey(), staleAuth);

        AuthorizationSeeder seeder = new AuthorizationSeeder(fakeAuthRepo);
        seeder.seed();

        // Verify stale items were removed
        assertNull(fakeAuthRepo.findByKey("OBSOLETE_ENDPOINT"), "Stale authorization should be removed");
        assertTrue(fakeAuthRepo.removeCalls > 0);
    }
}
