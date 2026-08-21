package com.freelance;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import com.freelance.configuration.dto.ErrorConfigEntry;
import com.freelance.manager.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationEngineTest {

    private static final String FIXTURE_PACKAGE = "com.freelance.fixtures";

    // ── Dummy test enum / records ─────────────────────────────────────────────

    private enum TestConfigs implements IConfig {
        STR_CONFIG("test.str", "default_str", "Test string config"),
        INT_CONFIG("test.int", "10", "Test int config"),
        LONG_CONFIG("test.long", "3600", "Test long config"),
        BOOL_CONFIG("test.bool", "true", "Test bool config");

        private final String key;
        private final String defaultValue;
        private final String description;

        TestConfigs(String key, String defaultValue, String description) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.description = description;
        }

        @Override public String getKey() { return key; }
        @Override public String getDefaultValue() { return defaultValue; }
        @Override public String getDescription() { return description; }
    }

    private enum TestErrors implements IError {
        SAMPLE_ERROR("test.sample.error", "FL_9001", 400, "Default error message", "Sample description"),
        NOT_FOUND_ERROR("test.not.found", "FL_9002", 404, "Not found message", "Not found description");

        private final String errorKey;
        private final String indirectionCode;
        private final int defaultStatusCode;
        private final String defaultMessage;
        private final String description;

        TestErrors(String errorKey, String indirectionCode, int defaultStatusCode, String defaultMessage, String description) {
            this.errorKey = errorKey;
            this.indirectionCode = indirectionCode;
            this.defaultStatusCode = defaultStatusCode;
            this.defaultMessage = defaultMessage;
            this.description = description;
        }

        @Override public String getErrorKey() { return errorKey; }
        @Override public String getIndirectionCode() { return indirectionCode; }
        @Override public int getDefaultStatusCode() { return defaultStatusCode; }
        @Override public String getDefaultMessage() { return defaultMessage; }
        @Override public String getDescription() { return description; }
    }

    // ── Reset CacheManager before each test ───────────────────────────────────

    @BeforeEach
    void setup() throws Exception {
        CacheManager mgr = CacheManager.getInstance();

        setField(mgr, "initialized", false);
        clearMap(mgr, "configsByName");
        clearMap(mgr, "nameByClass");
        clearMap(mgr, "caches");

        mgr.init(FIXTURE_PACKAGE);
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

    // ── get(IConfig) ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("get(IConfig)")
    class GetStringTests {

        @Test
        @DisplayName("returns cached value when present in cache")
        void returnsCachedValue() {
            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.STR_CONFIG.getKey(), "custom_value");
            assertEquals("custom_value", ConfigurationEngine.get(TestConfigs.STR_CONFIG));
        }

        @Test
        @DisplayName("falls back to default value when cache returns null")
        void fallbackOnNullCache() {
            assertEquals("default_str", ConfigurationEngine.get(TestConfigs.STR_CONFIG));
        }

        @Test
        @DisplayName("falls back to default value when cache returns blank string")
        void fallbackOnBlankCache() {
            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.STR_CONFIG.getKey(), "   ");
            assertEquals("default_str", ConfigurationEngine.get(TestConfigs.STR_CONFIG));
        }

        @Test
        @DisplayName("returns null when IConfig argument is null")
        void returnsNullWhenConfigNull() {
            assertNull(ConfigurationEngine.get(null));
        }
    }

    // ── getInt(IConfig) ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getInt(IConfig)")
    class GetIntTests {

        @Test
        @DisplayName("parses cached int value when valid")
        void parsesCachedInt() {
            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.INT_CONFIG.getKey(), "42");
            assertEquals(42, ConfigurationEngine.getInt(TestConfigs.INT_CONFIG));
        }

        @Test
        @DisplayName("falls back to default int when cache is empty")
        void fallbackOnEmptyCache() {
            assertEquals(10, ConfigurationEngine.getInt(TestConfigs.INT_CONFIG));
        }

        @Test
        @DisplayName("falls back to default int when cached value is non-numeric")
        void fallbackOnInvalidNumber() {
            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.INT_CONFIG.getKey(), "not-a-number");
            assertEquals(10, ConfigurationEngine.getInt(TestConfigs.INT_CONFIG));
        }
    }

    // ── getLong(IConfig) ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLong(IConfig)")
    class GetLongTests {

        @Test
        @DisplayName("parses cached long value when valid")
        void parsesCachedLong() {
            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.LONG_CONFIG.getKey(), "86400");
            assertEquals(86400L, ConfigurationEngine.getLong(TestConfigs.LONG_CONFIG));
        }

        @Test
        @DisplayName("falls back to default long when cache is empty")
        void fallbackOnEmptyCache() {
            assertEquals(3600L, ConfigurationEngine.getLong(TestConfigs.LONG_CONFIG));
        }

        @Test
        @DisplayName("falls back to default long when cached value is non-numeric")
        void fallbackOnInvalidNumber() {
            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.LONG_CONFIG.getKey(), "invalid-long");
            assertEquals(3600L, ConfigurationEngine.getLong(TestConfigs.LONG_CONFIG));
        }
    }

    // ── getBoolean(IConfig) ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getBoolean(IConfig)")
    class GetBooleanTests {

        @Test
        @DisplayName("parses cached boolean string correctly")
        void parsesCachedBoolean() {
            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.BOOL_CONFIG.getKey(), "false");
            assertFalse(ConfigurationEngine.getBoolean(TestConfigs.BOOL_CONFIG));

            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.BOOL_CONFIG.getKey(), "true");
            assertTrue(ConfigurationEngine.getBoolean(TestConfigs.BOOL_CONFIG));
        }

        @Test
        @DisplayName("falls back to default boolean when cache is empty")
        void fallbackOnEmptyCache() {
            assertTrue(ConfigurationEngine.getBoolean(TestConfigs.BOOL_CONFIG));
        }
    }

    // ── getStatusCode(IError) ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getStatusCode(IError)")
    class GetStatusCodeTests {

        @Test
        @DisplayName("returns cached status code when present and > 0")
        void returnsCachedStatusCode() {
            CacheManager.getInstance().put("cache_ErrorMessage", TestErrors.SAMPLE_ERROR.getErrorKey(),
                    new ErrorConfigEntry(422, "Unprocessable"));
            assertEquals(422, ConfigurationEngine.getStatusCode(TestErrors.SAMPLE_ERROR));
        }

        @Test
        @DisplayName("falls back to default status code when cache is empty")
        void fallbackOnEmptyCache() {
            assertEquals(400, ConfigurationEngine.getStatusCode(TestErrors.SAMPLE_ERROR));
        }

        @Test
        @DisplayName("falls back to default status code when cached status code is 0 or negative")
        void fallbackOnZeroStatusCode() {
            CacheManager.getInstance().put("cache_ErrorMessage", TestErrors.SAMPLE_ERROR.getErrorKey(),
                    new ErrorConfigEntry(0, "Some msg"));
            assertEquals(400, ConfigurationEngine.getStatusCode(TestErrors.SAMPLE_ERROR));
        }

        @Test
        @DisplayName("returns 500 when error is null")
        void returns500WhenNull() {
            assertEquals(500, ConfigurationEngine.getStatusCode(null));
        }
    }

    // ── getErrorMessage(IError) ───────────────────────────────────────────────

    @Nested
    @DisplayName("getErrorMessage(IError)")
    class GetErrorMessageTests {

        @Test
        @DisplayName("returns cached error message when present and non-blank")
        void returnsCachedErrorMessage() {
            CacheManager.getInstance().put("cache_ErrorMessage", TestErrors.SAMPLE_ERROR.getErrorKey(),
                    new ErrorConfigEntry(400, "Custom database message"));
            assertEquals("Custom database message", ConfigurationEngine.getErrorMessage(TestErrors.SAMPLE_ERROR));
        }

        @Test
        @DisplayName("falls back to default message when cache is empty")
        void fallbackOnEmptyCache() {
            assertEquals("Default error message", ConfigurationEngine.getErrorMessage(TestErrors.SAMPLE_ERROR));
        }

        @Test
        @DisplayName("falls back to default message when cached message is blank")
        void fallbackOnBlankMessage() {
            CacheManager.getInstance().put("cache_ErrorMessage", TestErrors.SAMPLE_ERROR.getErrorKey(),
                    new ErrorConfigEntry(400, "   "));
            assertEquals("Default error message", ConfigurationEngine.getErrorMessage(TestErrors.SAMPLE_ERROR));
        }

        @Test
        @DisplayName("returns null when error is null")
        void returnsNullWhenErrorNull() {
            assertNull(ConfigurationEngine.getErrorMessage(null));
        }
    }

    // ── Invalidation Tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("invalidateConfig(IConfig) and invalidateError(IError)")
    class InvalidationTests {

        @Test
        @DisplayName("invalidateConfig removes item from cache_SystemConfig")
        void invalidatesSystemConfig() {
            CacheManager.getInstance().put("cache_SystemConfig", TestConfigs.STR_CONFIG.getKey(), "to_be_evicted");
            assertEquals("to_be_evicted", ConfigurationEngine.get(TestConfigs.STR_CONFIG));

            ConfigurationEngine.invalidateConfig(TestConfigs.STR_CONFIG);

            // After eviction, fallback is returned
            assertEquals("default_str", ConfigurationEngine.get(TestConfigs.STR_CONFIG));
        }

        @Test
        @DisplayName("invalidateConfig with null does not throw")
        void invalidateConfigNullSafe() {
            assertDoesNotThrow(() -> ConfigurationEngine.invalidateConfig(null));
        }

        @Test
        @DisplayName("invalidateError removes item from cache_ErrorMessage")
        void invalidatesErrorMessage() {
            CacheManager.getInstance().put("cache_ErrorMessage", TestErrors.SAMPLE_ERROR.getErrorKey(),
                    new ErrorConfigEntry(400, "evict_me"));
            assertEquals("evict_me", ConfigurationEngine.getErrorMessage(TestErrors.SAMPLE_ERROR));

            ConfigurationEngine.invalidateError(TestErrors.SAMPLE_ERROR);

            // After eviction, fallback is returned
            assertEquals("Default error message", ConfigurationEngine.getErrorMessage(TestErrors.SAMPLE_ERROR));
        }

        @Test
        @DisplayName("invalidateError with null does not throw")
        void invalidateErrorNullSafe() {
            assertDoesNotThrow(() -> ConfigurationEngine.invalidateError(null));
        }
    }
}
