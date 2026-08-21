package com.freelance.configuration;

import com.freelance.cache.config.ErrorMessageCache;
import com.freelance.cache.config.SystemConfigurationCache;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.configuration.entity.error.ErrorConfiguration;
import com.freelance.configuration.entity.system.SystemConfiguration;
import com.freelance.configuration.error.ErrorConfigurationService;
import com.freelance.configuration.repository.error.ErrorConfigurationRepository;
import com.freelance.configuration.repository.system.SystemConfigurationRepository;
import com.freelance.configuration.system.SystemConfigurationService;
import com.freelance.manager.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationServiceAndCacheTest {

    // ── In-Memory Fakes ───────────────────────────────────────────────────────

    static class StubSystemConfigurationRepository extends SystemConfigurationRepository {
        final Map<String, SystemConfiguration> storage = new HashMap<>();

        @Override
        public Map<String, String> getAll() {
            Map<String, String> map = new HashMap<>();
            storage.forEach((k, v) -> map.put(k, v.getConfigValue()));
            return map;
        }

        @Override
        public SystemConfiguration get(String configId) {
            return storage.get(configId);
        }
    }

    static class StubErrorConfigurationRepository extends ErrorConfigurationRepository {
        final Map<String, ErrorConfiguration> storage = new HashMap<>();

        @Override
        public Map<String, ErrorConfigEntry> getAllErrorEntries() {
            Map<String, ErrorConfigEntry> map = new HashMap<>();
            storage.forEach((k, v) -> map.put(k, new ErrorConfigEntry(v.getStatusCode(), v.getErrorMessage())));
            return map;
        }

        @Override
        public ErrorConfiguration get(String errorId) {
            return storage.get(errorId);
        }
    }

    // ── CacheManager Setup ────────────────────────────────────────────────────

    @BeforeEach
    void setupCacheManager() throws Exception {
        CacheManager mgr = CacheManager.getInstance();

        setField(mgr, "initialized", false);
        clearMap(mgr, "configsByName");
        clearMap(mgr, "nameByClass");
        clearMap(mgr, "caches");

        mgr.init("com.freelance.cache.config");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @SuppressWarnings("unchecked")
    private static void clearMap(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        ((Map<?, ?>) f.get(target)).clear();
    }

    // ── Cache Definitions ─────────────────────────────────────────────────────

    @Test
    @DisplayName("SystemConfigurationCache definition name is cache_SystemConfig")
    void systemConfigCache_nameMatches() {
        SystemConfigurationCache cache = new SystemConfigurationCache();
        assertEquals("cache_SystemConfig", cache.toDefinition().name());
    }

    @Test
    @DisplayName("ErrorMessageCache definition name is cache_ErrorMessage")
    void errorMessageCache_nameMatches() {
        ErrorMessageCache cache = new ErrorMessageCache();
        assertEquals("cache_ErrorMessage", cache.toDefinition().name());
    }

    // ── SystemConfigurationService ────────────────────────────────────────────

    @Nested
    @DisplayName("SystemConfigurationService")
    class SystemConfigServiceTests {

        @Test
        @DisplayName("loadConfigValue returns value when entity exists")
        void loadConfigValue_found() {
            StubSystemConfigurationRepository stubRepo = new StubSystemConfigurationRepository();
            stubRepo.storage.put("test.key", new SystemConfiguration("test.key", "my_val", "desc"));

            SystemConfigurationService service = new SystemConfigurationService(stubRepo);
            assertEquals("my_val", service.loadConfigValue("test.key"));
        }

        @Test
        @DisplayName("loadConfigValue returns null when entity is absent")
        void loadConfigValue_notFound() {
            StubSystemConfigurationRepository stubRepo = new StubSystemConfigurationRepository();

            SystemConfigurationService service = new SystemConfigurationService(stubRepo);
            assertNull(service.loadConfigValue("missing.key"));
        }

        @Test
        @DisplayName("preloadSystemCacheEntries populates CacheManager with all repo values")
        void preloadSystemCacheEntries_populatesCache() {
            StubSystemConfigurationRepository stubRepo = new StubSystemConfigurationRepository();
            stubRepo.storage.put("k1", new SystemConfiguration("k1", "v1", "desc1"));
            stubRepo.storage.put("k2", new SystemConfiguration("k2", "v2", "desc2"));

            SystemConfigurationService service = new SystemConfigurationService(stubRepo);
            service.preloadSystemCacheEntries();

            assertEquals("v1", CacheManager.getInstance().get("cache_SystemConfig", "k1"));
            assertEquals("v2", CacheManager.getInstance().get("cache_SystemConfig", "k2"));
        }
    }

    // ── ErrorConfigurationService ─────────────────────────────────────────────

    @Nested
    @DisplayName("ErrorConfigurationService")
    class ErrorConfigServiceTests {

        @Test
        @DisplayName("loadErrorConfig returns ErrorConfigEntry when entity exists")
        void loadErrorConfig_found() {
            StubErrorConfigurationRepository stubRepo = new StubErrorConfigurationRepository();
            stubRepo.storage.put("test.error", new ErrorConfiguration("test.error", 400, "Error msg", "desc"));

            ErrorConfigurationService service = new ErrorConfigurationService(stubRepo);
            ErrorConfigEntry entry = service.loadErrorConfig("test.error");

            assertNotNull(entry);
            assertEquals(400, entry.statusCode());
            assertEquals("Error msg", entry.errorMessage());
        }

        @Test
        @DisplayName("loadErrorConfig returns null when entity is absent")
        void loadErrorConfig_notFound() {
            StubErrorConfigurationRepository stubRepo = new StubErrorConfigurationRepository();

            ErrorConfigurationService service = new ErrorConfigurationService(stubRepo);
            assertNull(service.loadErrorConfig("missing.error"));
        }

        @Test
        @DisplayName("preloadErrorCacheEntries populates CacheManager with all repo error entries")
        void preloadErrorCacheEntries_populatesCache() {
            StubErrorConfigurationRepository stubRepo = new StubErrorConfigurationRepository();
            stubRepo.storage.put("err1", new ErrorConfiguration("err1", 400, "Msg1", "desc1"));
            stubRepo.storage.put("err2", new ErrorConfiguration("err2", 500, "Msg2", "desc2"));

            ErrorConfigurationService service = new ErrorConfigurationService(stubRepo);
            service.preloadErrorCacheEntries();

            ErrorConfigEntry entry1 = CacheManager.getInstance().get("cache_ErrorMessage", "err1");
            assertNotNull(entry1);
            assertEquals(400, entry1.statusCode());
            assertEquals("Msg1", entry1.errorMessage());

            ErrorConfigEntry entry2 = CacheManager.getInstance().get("cache_ErrorMessage", "err2");
            assertNotNull(entry2);
            assertEquals(500, entry2.statusCode());
            assertEquals("Msg2", entry2.errorMessage());
        }
    }
}
