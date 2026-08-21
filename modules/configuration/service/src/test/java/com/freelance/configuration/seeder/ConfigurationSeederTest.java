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
        int deleteSystemOwnedCalls = 0;
        List<SystemConfiguration> lastPersistedBatch = Collections.emptyList();

        @Override
        public Map<String, String> getAll() {
            Map<String, String> map = new HashMap<>();
            storage.forEach((k, v) -> map.put(k, v.getConfigValue()));
            return map;
        }

        @Override
        public Set<String> findAllIds() {
            return new HashSet<>(storage.keySet());
        }

        @Override
        public int deleteSystemOwned() {
            deleteSystemOwnedCalls++;
            return 5;
        }

        @Override
        public void persistBatch(List<SystemConfiguration> entities, int batchSize) {
            this.lastPersistedBatch = new ArrayList<>(entities);
            for (SystemConfiguration e : entities) {
                storage.put(e.getConfigId(), e);
            }
        }

        @Override
        public SystemConfiguration get(String configId) {
            return storage.get(configId);
        }
    }

    static class FakeErrorConfigurationRepository extends ErrorConfigurationRepository {
        final Map<String, ErrorConfiguration> storage = new HashMap<>();
        int deleteSystemOwnedCalls = 0;
        UUID lastSystemUserId = null;
        List<ErrorConfiguration> lastPersistedBatch = Collections.emptyList();

        @Override
        public Map<String, ErrorConfigEntry> getAllErrorEntries() {
            Map<String, ErrorConfigEntry> map = new HashMap<>();
            storage.forEach((k, v) -> map.put(k, new ErrorConfigEntry(v.getStatusCode(), v.getErrorMessage())));
            return map;
        }

        @Override
        public Set<String> findAllIds() {
            return new HashSet<>(storage.keySet());
        }

        @Override
        public int deleteSystemOwned(UUID systemUserId) {
            deleteSystemOwnedCalls++;
            this.lastSystemUserId = systemUserId;
            return 10;
        }

        @Override
        public void persistBatch(List<ErrorConfiguration> entities, int batchSize) {
            this.lastPersistedBatch = new ArrayList<>(entities);
            for (ErrorConfiguration e : entities) {
                storage.put(e.getErrorId(), e);
            }
        }

        @Override
        public ErrorConfiguration get(String errorId) {
            return storage.get(errorId);
        }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("seed() cleans system-owned rows and batch-inserts missing code defaults")
    void seed_cleansAndInsertsMissingEntries() {
        FakeSystemConfigurationRepository fakeSysRepo = new FakeSystemConfigurationRepository();
        FakeErrorConfigurationRepository fakeErrRepo = new FakeErrorConfigurationRepository();

        ConfigurationSeeder seeder = new ConfigurationSeeder(fakeSysRepo, fakeErrRepo);
        seeder.seed();

        assertEquals(1, fakeSysRepo.deleteSystemOwnedCalls);
        assertEquals(1, fakeErrRepo.deleteSystemOwnedCalls);
        assertEquals(SystemHardcoding.SYSTEM_USER_ID.asUuid(), fakeErrRepo.lastSystemUserId);

        List<IConfig> allConfigs = ConfigurationRegistry.getAllConfigs();
        assertEquals(allConfigs.size(), fakeSysRepo.lastPersistedBatch.size());
        assertEquals(allConfigs.size(), fakeSysRepo.storage.size());

        List<IError> allErrors = ConfigurationRegistry.getAllErrors();
        assertEquals(allErrors.size(), fakeErrRepo.lastPersistedBatch.size());
        assertEquals(allErrors.size(), fakeErrRepo.storage.size());
    }

    @Test
    @DisplayName("seed() skips keys that already exist in database (preserving admin-modified entries)")
    void seed_preservesExistingAdminEntries() {
        FakeSystemConfigurationRepository fakeSysRepo = new FakeSystemConfigurationRepository();
        FakeErrorConfigurationRepository fakeErrRepo = new FakeErrorConfigurationRepository();

        // Populate repos so all code keys already exist
        for (IConfig config : ConfigurationRegistry.getAllConfigs()) {
            fakeSysRepo.storage.put(config.getKey(),
                    new SystemConfiguration(config.getKey(), "admin_override", "custom desc"));
        }
        for (IError error : ConfigurationRegistry.getAllErrors()) {
            fakeErrRepo.storage.put(error.getErrorKey(),
                    new ErrorConfiguration(error.getErrorKey(), 418, "admin msg", "custom desc"));
        }

        ConfigurationSeeder seeder = new ConfigurationSeeder(fakeSysRepo, fakeErrRepo);
        seeder.seed();

        assertEquals(1, fakeSysRepo.deleteSystemOwnedCalls);
        assertEquals(1, fakeErrRepo.deleteSystemOwnedCalls);

        // Batch persisted lists should be empty since nothing is missing
        assertTrue(fakeSysRepo.lastPersistedBatch.isEmpty(),
                "Expected 0 new system configs to be inserted");
        assertTrue(fakeErrRepo.lastPersistedBatch.isEmpty(),
                "Expected 0 new error configs to be inserted");

        // Existing admin overrides must remain intact
        assertEquals("admin_override", fakeSysRepo.get(ConfigurationRegistry.getAllConfigs().get(0).getKey()).getConfigValue());
        assertEquals(418, fakeErrRepo.get(ConfigurationRegistry.getAllErrors().get(0).getErrorKey()).getStatusCode());
    }
}
