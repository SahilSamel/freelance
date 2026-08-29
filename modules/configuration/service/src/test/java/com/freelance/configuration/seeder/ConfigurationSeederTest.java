package com.freelance.configuration.seeder;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.configuration.entity.error.ErrorConfiguration;
import com.freelance.configuration.entity.system.SystemConfiguration;
import com.freelance.configuration.repository.error.ErrorConfigurationRepository;
import com.freelance.configuration.repository.system.SystemConfigurationRepository;
import com.freelance.system.SystemHardcoding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationSeederTest {

    // ── In-Memory Fakes ───────────────────────────────────────────────────────

    static class FakeSystemConfigurationRepository extends SystemConfigurationRepository {
        final Map<String, SystemConfiguration> storage = new HashMap<>();
        List<SystemConfiguration> lastPersistedBatch = Collections.emptyList();
        int mergeCalls = 0;
        int removeCalls = 0;

        @Override
        public Map<String, String> getAll() {
            Map<String, String> map = new HashMap<>();
            storage.forEach((k, v) -> map.put(k, v.getConfigValue()));
            return map;
        }

        @Override
        public Map<String, SystemConfiguration> findAllEntitiesAsMap() {
            return new HashMap<>(storage);
        }

        @Override
        public Set<String> findAllIds() {
            return new HashSet<>(storage.keySet());
        }

        @Override
        public void persistEntriesInBatch(List<SystemConfiguration> entities, int... batchSize) {
            this.lastPersistedBatch = new ArrayList<>(entities);
            for (SystemConfiguration e : entities) {
                storage.put(e.getConfigId(), e);
            }
        }

        @Override
        public List<SystemConfiguration> mergeEntriesInBatch(List<SystemConfiguration> entities, int... batchSize) {
            if (entities == null) return Collections.emptyList();
            mergeCalls += entities.size();
            for (SystemConfiguration e : entities) {
                storage.put(e.getConfigId(), e);
            }
            return entities;
        }

        @Override
        public void removeEntriesInBatch(List<SystemConfiguration> entities, int... batchSize) {
            if (entities != null) {
                removeCalls += entities.size();
                for (SystemConfiguration e : entities) {
                    storage.remove(e.getConfigId());
                }
            }
        }

        @Override
        public SystemConfiguration get(String configId) {
            return storage.get(configId);
        }
    }

    static class FakeErrorConfigurationRepository extends ErrorConfigurationRepository {
        final Map<String, ErrorConfiguration> storage = new HashMap<>();
        List<ErrorConfiguration> lastPersistedBatch = Collections.emptyList();
        int mergeCalls = 0;
        int removeCalls = 0;

        @Override
        public Map<String, ErrorConfigEntry> getAllErrorEntries() {
            Map<String, ErrorConfigEntry> map = new HashMap<>();
            storage.forEach((k, v) -> map.put(k, new ErrorConfigEntry(v.getStatusCode(), v.getErrorMessage())));
            return map;
        }

        @Override
        public Map<String, ErrorConfiguration> findAllEntitiesAsMap() {
            return new HashMap<>(storage);
        }

        @Override
        public List<ErrorConfiguration> findAllEntities() {
            return new ArrayList<>(storage.values());
        }

        @Override
        public void persistEntriesInBatch(List<ErrorConfiguration> entities, int... batchSize) {
            this.lastPersistedBatch = new ArrayList<>(entities);
            for (ErrorConfiguration e : entities) {
                storage.put(e.getErrorId(), e);
            }
        }

        @Override
        public List<ErrorConfiguration> mergeEntriesInBatch(List<ErrorConfiguration> entities, int... batchSize) {
            if (entities == null) return Collections.emptyList();
            mergeCalls += entities.size();
            for (ErrorConfiguration e : entities) {
                storage.put(e.getErrorId(), e);
            }
            return entities;
        }

        @Override
        public void removeEntriesInBatch(List<ErrorConfiguration> entities, int... batchSize) {
            if (entities != null) {
                removeCalls += entities.size();
                for (ErrorConfiguration e : entities) {
                    storage.remove(e.getErrorId());
                }
            }
        }

        @Override
        public ErrorConfiguration get(String errorId) {
            return storage.get(errorId);
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("seed() discovers and batch-inserts missing code defaults on initial startup")
    void seed_cleansAndInsertsMissingEntries() {
        FakeSystemConfigurationRepository fakeSysRepo = new FakeSystemConfigurationRepository();
        FakeErrorConfigurationRepository fakeErrRepo = new FakeErrorConfigurationRepository();

        ConfigurationSeeder seeder = new ConfigurationSeeder(fakeSysRepo, fakeErrRepo);
        seeder.seed();

        List<IConfig> allConfigs = ConfigurationRegistry.getAllConfigs();
        assertEquals(allConfigs.size(), fakeSysRepo.lastPersistedBatch.size());
        assertEquals(allConfigs.size(), fakeSysRepo.storage.size());

        List<IError> allErrors = ConfigurationRegistry.getAllErrors();
        assertEquals(allErrors.size(), fakeErrRepo.lastPersistedBatch.size());
        assertEquals(allErrors.size(), fakeErrRepo.storage.size());
    }

    @Test
    @DisplayName("seed() is idempotent: skips database writes entirely when code definitions match database")
    void seed_isIdempotentAndSkipsUnchangedRuns() {
        FakeSystemConfigurationRepository fakeSysRepo = new FakeSystemConfigurationRepository();
        FakeErrorConfigurationRepository fakeErrRepo = new FakeErrorConfigurationRepository();

        ConfigurationSeeder seeder = new ConfigurationSeeder(fakeSysRepo, fakeErrRepo);

        // First run: inserts all defaults
        seeder.seed();
        int initialSysCount = fakeSysRepo.storage.size();
        int initialErrCount = fakeErrRepo.storage.size();

        // Reset trackers
        fakeSysRepo.lastPersistedBatch = Collections.emptyList();
        fakeErrRepo.lastPersistedBatch = Collections.emptyList();
        fakeSysRepo.mergeCalls = 0;
        fakeErrRepo.mergeCalls = 0;
        fakeSysRepo.removeCalls = 0;
        fakeErrRepo.removeCalls = 0;

        // Second run: 0 changes
        seeder.seed();

        assertEquals(initialSysCount, fakeSysRepo.storage.size());
        assertEquals(initialErrCount, fakeErrRepo.storage.size());

        assertTrue(fakeSysRepo.lastPersistedBatch.isEmpty(), "Expected 0 new system configs inserted on second run");
        assertTrue(fakeErrRepo.lastPersistedBatch.isEmpty(), "Expected 0 new error configs inserted on second run");
        assertEquals(0, fakeSysRepo.mergeCalls, "Expected 0 merge calls when configs are unchanged");
        assertEquals(0, fakeErrRepo.mergeCalls, "Expected 0 merge calls when errors are unchanged");
        assertEquals(0, fakeSysRepo.removeCalls, "Expected 0 remove calls on unchanged run");
        assertEquals(0, fakeErrRepo.removeCalls, "Expected 0 remove calls on unchanged run");
    }

    @Test
    @DisplayName("seed() preserves administrator custom values while updating code descriptions")
    void seed_preservesExistingAdminEntriesWhileUpdatingDescriptions() {
        FakeSystemConfigurationRepository fakeSysRepo = new FakeSystemConfigurationRepository();
        FakeErrorConfigurationRepository fakeErrRepo = new FakeErrorConfigurationRepository();
        UUID adminUserId = UUID.randomUUID();

        // Populate repos so all code keys already exist with admin values and outdated descriptions
        for (IConfig config : ConfigurationRegistry.getAllConfigs()) {
            SystemConfiguration sc = new SystemConfiguration(config.getKey(), "admin_custom_val", "outdated desc");
            sc.setLastUpdatedBy(adminUserId);
            fakeSysRepo.storage.put(config.getKey(), sc);
        }
        for (IError error : ConfigurationRegistry.getAllErrors()) {
            ErrorConfiguration ec = new ErrorConfiguration(error.getErrorKey(), 418, "admin custom message", "outdated desc");
            ec.setLastUpdatedBy(adminUserId);
            fakeErrRepo.storage.put(error.getErrorKey(), ec);
        }

        ConfigurationSeeder seeder = new ConfigurationSeeder(fakeSysRepo, fakeErrRepo);
        seeder.seed();

        // Batch persisted lists should be empty since keys already existed
        assertTrue(fakeSysRepo.lastPersistedBatch.isEmpty(), "Expected 0 new system configs to be inserted");
        assertTrue(fakeErrRepo.lastPersistedBatch.isEmpty(), "Expected 0 new error configs to be inserted");

        // Existing admin overrides must remain intact
        var sampleConfig = ConfigurationRegistry.getAllConfigs().get(0);
        assertEquals("admin_custom_val", fakeSysRepo.get(sampleConfig.getKey()).getConfigValue());
        assertEquals(sampleConfig.getDescription(), fakeSysRepo.get(sampleConfig.getKey()).getDescription(),
                "Description should be updated to match code definition");

        var sampleError = ConfigurationRegistry.getAllErrors().get(0);
        assertEquals(418, fakeErrRepo.get(sampleError.getErrorKey()).getStatusCode());
        assertEquals("admin custom message", fakeErrRepo.get(sampleError.getErrorKey()).getErrorMessage());
        assertEquals(sampleError.getDescription(), fakeErrRepo.get(sampleError.getErrorKey()).getDescription(),
                "Error description should be updated to match code definition");
    }

    @Test
    @DisplayName("seed() deletes stale system-owned configurations and error messages no longer in code")
    void seed_deletesStaleSystemOwnedEntries() {
        FakeSystemConfigurationRepository fakeSysRepo = new FakeSystemConfigurationRepository();
        FakeErrorConfigurationRepository fakeErrRepo = new FakeErrorConfigurationRepository();
        UUID systemUserId = SystemHardcoding.SYSTEM_USER_ID.asUuid();

        // Inject stale system-owned config and error
        SystemConfiguration staleConfig = new SystemConfiguration("obsolete.config.key", "val", "desc");
        staleConfig.setLastUpdatedBy(systemUserId);
        fakeSysRepo.storage.put(staleConfig.getConfigId(), staleConfig);

        ErrorConfiguration staleError = new ErrorConfiguration("OBSOLETE_ERROR", 500, "msg", "desc");
        staleError.setLastUpdatedBy(systemUserId);
        fakeErrRepo.storage.put(staleError.getErrorId(), staleError);

        ConfigurationSeeder seeder = new ConfigurationSeeder(fakeSysRepo, fakeErrRepo);
        seeder.seed();

        assertNull(fakeSysRepo.get("obsolete.config.key"), "Stale system config should be deleted");
        assertNull(fakeErrRepo.get("OBSOLETE_ERROR"), "Stale system error should be deleted");
        assertTrue(fakeSysRepo.removeCalls > 0);
        assertTrue(fakeErrRepo.removeCalls > 0);
    }
}
