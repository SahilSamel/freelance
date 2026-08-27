package com.freelance.authorization.seeder;

import com.freelance.authorization.SystemRoles;
import com.freelance.entity.authorization.Authorization;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorization;
import com.freelance.entity.mapping.roleAuthorization.RoleAuthorizationKey;
import com.freelance.entity.role.Role;
import com.freelance.repository.authorization.AuthorizationRepository;
import com.freelance.repository.mapping.roleAuthorization.RoleAuthorizationRepository;
import com.freelance.repository.role.RoleRepository;
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

    static class FakeRoleRepository extends RoleRepository {
        final Map<String, Role> storage = new HashMap<>();
        List<Role> lastPersistedBatch = Collections.emptyList();
        int removeCalls = 0;

        @Override
        public Role findByName(String name) {
            return storage.get(name);
        }

        @Override
        public List<Role> findAllSystemRoles() {
            return storage.values().stream().filter(Role::isSystem).toList();
        }

        @Override
        public Map<String, Role> findAllSystemRolesAsMap() {
            Map<String, Role> map = new HashMap<>();
            storage.values().stream().filter(Role::isSystem).forEach(r -> map.put(r.getName(), r));
            return map;
        }

        @Override
        public void persistEntriesInBatch(List<Role> entities, int... batchSize) {
            this.lastPersistedBatch = new ArrayList<>(entities);
            for (Role r : entities) {
                storage.put(r.getName(), r);
            }
        }

        @Override
        public void remove(Role entity) {
            if (entity != null) {
                removeCalls++;
                storage.remove(entity.getName());
            }
        }

        @Override
        public void removeEntriesInBatch(List<Role> entities, int... batchSize) {
            if (entities != null) {
                for (Role r : entities) {
                    remove(r);
                }
            }
        }
    }

    static class FakeRoleAuthorizationRepository extends RoleAuthorizationRepository {
        final Map<RoleAuthorizationKey, RoleAuthorization> storage = new HashMap<>();
        List<RoleAuthorization> lastPersistedBatch = Collections.emptyList();
        int removeCalls = 0;

        @Override
        public void persistEntriesInBatch(List<RoleAuthorization> entities, int... batchSize) {
            this.lastPersistedBatch = new ArrayList<>(entities);
            for (RoleAuthorization ra : entities) {
                storage.put(ra.getId(), ra);
            }
        }

        @Override
        public void remove(RoleAuthorization entity) {
            if (entity != null) {
                removeCalls++;
                storage.remove(entity.getId());
            }
        }

        @Override
        public void removeEntriesInBatch(List<RoleAuthorization> entities, int... batchSize) {
            if (entities != null) {
                for (RoleAuthorization ra : entities) {
                    remove(ra);
                }
            }
        }

        @Override
        public Map<RoleAuthorizationKey, RoleAuthorization> findAllSystemRoleMappings() {
            Map<RoleAuthorizationKey, RoleAuthorization> map = new HashMap<>();
            for (RoleAuthorization ra : storage.values()) {
                if (ra.getRole() != null && ra.getRole().isSystem()) {
                    map.put(ra.getId(), ra);
                }
            }
            return map;
        }

        @Override
        public Map<UUID, List<RoleAuthorization>> getAllMappingsGroupedByRoleId() {
            Map<UUID, List<RoleAuthorization>> map = new HashMap<>();
            for (RoleAuthorization ra : storage.values()) {
                map.computeIfAbsent(ra.getId().getRoleId(), k -> new ArrayList<>()).add(ra);
            }
            return map;
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("seed() discovers endpoint @Authorization annotations and batch-persists entities")
    void seed_discoversAndPersistsEntities() {
        FakeAuthorizationRepository fakeAuthRepo = new FakeAuthorizationRepository();
        FakeRoleRepository fakeRoleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository fakeRoleAuthRepo = new FakeRoleAuthorizationRepository();

        AuthorizationSeeder seeder = new AuthorizationSeeder(fakeAuthRepo, fakeRoleRepo, fakeRoleAuthRepo);
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

        // Verify System Roles persisted for any annotations declaring a role
        long distinctSystemRolesCount = annotations.stream()
                .filter(a -> a.role() != null && a.role() != SystemRoles.NONE)
                .map(a -> a.role().name())
                .distinct()
                .count();

        assertEquals(distinctSystemRolesCount, fakeRoleRepo.storage.size());

        // Verify Mappings persisted
        long roleAuthAnnotationCount = annotations.stream()
                .filter(a -> a.role() != null && a.role() != SystemRoles.NONE)
                .count();

        assertEquals(roleAuthAnnotationCount, fakeRoleAuthRepo.storage.size());
    }

    @Test
    @DisplayName("seed() is idempotent: when no code changes exist, 0 DB modifications occur on second run")
    void seed_isIdempotentAndSkipsUnchangedMappings() {
        FakeAuthorizationRepository fakeAuthRepo = new FakeAuthorizationRepository();
        FakeRoleRepository fakeRoleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository fakeRoleAuthRepo = new FakeRoleAuthorizationRepository();

        AuthorizationSeeder seeder = new AuthorizationSeeder(fakeAuthRepo, fakeRoleRepo, fakeRoleAuthRepo);

        // Run seed first time
        seeder.seed();
        int initialAuthCount = fakeAuthRepo.storage.size();
        int initialRoleCount = fakeRoleRepo.storage.size();
        int initialMappingCount = fakeRoleAuthRepo.storage.size();

        // Clear persisted batch trackers
        fakeAuthRepo.lastPersistedBatch = Collections.emptyList();
        fakeRoleRepo.lastPersistedBatch = Collections.emptyList();
        fakeRoleAuthRepo.lastPersistedBatch = Collections.emptyList();
        fakeAuthRepo.mergeCalls = 0;
        fakeRoleAuthRepo.removeCalls = 0;

        // Run seed second time (0 changes in code)
        seeder.seed();

        // Counts unchanged
        assertEquals(initialAuthCount, fakeAuthRepo.storage.size());
        assertEquals(initialRoleCount, fakeRoleRepo.storage.size());
        assertEquals(initialMappingCount, fakeRoleAuthRepo.storage.size());

        // Zero mutations executed on second run
        assertTrue(fakeAuthRepo.lastPersistedBatch.isEmpty(), "Expected 0 new authorizations persisted");
        assertTrue(fakeRoleRepo.lastPersistedBatch.isEmpty(), "Expected 0 new roles persisted");
        assertTrue(fakeRoleAuthRepo.lastPersistedBatch.isEmpty(), "Expected 0 new mappings persisted");
        assertEquals(0, fakeAuthRepo.mergeCalls, "Expected 0 merge calls when metadata is unchanged");
        assertEquals(0, fakeRoleAuthRepo.removeCalls, "Expected 0 remove calls for mappings when unchanged");
    }

    @Test
    @DisplayName("seed() detects and updates changed authorization descriptions/names")
    void seed_updatesMetadataWhenChanged() {
        FakeAuthorizationRepository fakeAuthRepo = new FakeAuthorizationRepository();
        FakeRoleRepository fakeRoleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository fakeRoleAuthRepo = new FakeRoleAuthorizationRepository();

        AuthorizationSeeder seeder = new AuthorizationSeeder(fakeAuthRepo, fakeRoleRepo, fakeRoleAuthRepo);
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
    @DisplayName("seed() deletes stale authorizations and system roles that were removed from code")
    void seed_deletesStaleEntriesNoLongerInCode() {
        FakeAuthorizationRepository fakeAuthRepo = new FakeAuthorizationRepository();
        FakeRoleRepository fakeRoleRepo = new FakeRoleRepository();
        FakeRoleAuthorizationRepository fakeRoleAuthRepo = new FakeRoleAuthorizationRepository();

        // Inject stale authorization and stale system role into storage
        Authorization staleAuth = new Authorization(
                UUID.randomUUID(), "OBSOLETE_ENDPOINT", "Obsolete", "No longer used", "Obsolete"
        );
        fakeAuthRepo.storage.put(staleAuth.getKey(), staleAuth);

        Role staleRole = new Role(
                UUID.randomUUID(), "OBSOLETE_ROLE", "Obsolete role", true
        );
        fakeRoleRepo.storage.put(staleRole.getName(), staleRole);

        RoleAuthorization staleMapping = new RoleAuthorization(staleRole, staleAuth);
        fakeRoleAuthRepo.storage.put(staleMapping.getId(), staleMapping);

        AuthorizationSeeder seeder = new AuthorizationSeeder(fakeAuthRepo, fakeRoleRepo, fakeRoleAuthRepo);
        seeder.seed();

        // Verify stale items were removed
        assertNull(fakeAuthRepo.findByKey("OBSOLETE_ENDPOINT"), "Stale authorization should be removed");
        assertNull(fakeRoleRepo.findByName("OBSOLETE_ROLE"), "Stale system role should be removed");
        assertFalse(fakeRoleAuthRepo.storage.containsKey(staleMapping.getId()), "Stale mapping should be removed");

        assertTrue(fakeAuthRepo.removeCalls > 0);
        assertTrue(fakeRoleRepo.removeCalls > 0);
        assertTrue(fakeRoleAuthRepo.removeCalls > 0);
    }
}
