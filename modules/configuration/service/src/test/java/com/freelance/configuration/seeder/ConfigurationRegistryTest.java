package com.freelance.configuration.seeder;

import com.freelance.config.IConfig;
import com.freelance.config.IError;
import com.freelance.registry.ConfigurationRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationRegistryTest {

    @Test
    @DisplayName("getAllConfigs discovers valid and complete IConfig enums")
    void getAllConfigs_returnsValidConfigs() {
        List<IConfig> configs = ConfigurationRegistry.getAllConfigs();

        assertNotNull(configs);
        assertFalse(configs.isEmpty(), "Expected to discover at least one IConfig enum");

        Set<String> uniqueKeys = new HashSet<>();

        for (IConfig config : configs) {
            assertNotNull(config.getKey(), "IConfig key cannot be null for " + config);
            assertFalse(config.getKey().isBlank(), "IConfig key cannot be blank for " + config);

            assertNotNull(config.getDefaultValue(), "IConfig default value cannot be null for " + config);
            assertNotNull(config.getDescription(), "IConfig description cannot be null for " + config);

            assertTrue(uniqueKeys.add(config.getKey()),
                    "Duplicate IConfig key detected: '" + config.getKey() + "' in " + config);
        }
    }

    @Test
    @DisplayName("getAllErrors discovers valid and complete IError enums")
    void getAllErrors_returnsValidErrors() {
        List<IError> errors = ConfigurationRegistry.getAllErrors();

        assertNotNull(errors);
        assertFalse(errors.isEmpty(), "Expected to discover at least one IError enum");

        for (IError error : errors) {
            assertNotNull(error.getErrorKey(), "IError errorKey cannot be null for " + error);
            assertFalse(error.getErrorKey().isBlank(), "IError errorKey cannot be blank for " + error);

            assertNotNull(error.getIndirectionCode(), "IError indirectionCode cannot be null for " + error);
            assertFalse(error.getIndirectionCode().isBlank(), "IError indirectionCode cannot be blank for " + error);

            assertNotNull(error.getDefaultMessage(), "IError defaultMessage cannot be null for " + error);
            assertFalse(error.getDefaultMessage().isBlank(), "IError defaultMessage cannot be blank for " + error);

            assertNotNull(error.getDescription(), "IError description cannot be null for " + error);

            assertTrue(error.getDefaultStatusCode() >= 100 && error.getDefaultStatusCode() <= 599,
                    "Invalid HTTP status code for " + error + ": " + error.getDefaultStatusCode());
        }
    }

    @Test
    @DisplayName("Guarantees all IError indirectionCodes (FL_XXXX) are globally unique across all modules")
    void assertAllErrorIndirectionCodesAreUnique() {
        List<IError> errors = ConfigurationRegistry.getAllErrors();
        Map<String, IError> seenCodes = new HashMap<>();

        for (IError error : errors) {
            String code = error.getIndirectionCode();
            IError existing = seenCodes.put(code, error);

            assertNull(existing, String.format(
                    "DUPLICATE ERROR CODE DETECTED: '%s' is used by both [%s] and [%s]! Each error must have a unique code.",
                    code, existing, error
            ));
        }
    }

    @Test
    @DisplayName("Guarantees all IError errorKeys are globally unique across all modules")
    void assertAllErrorKeysAreUnique() {
        List<IError> errors = ConfigurationRegistry.getAllErrors();
        Map<String, IError> seenKeys = new HashMap<>();

        for (IError error : errors) {
            String key = error.getErrorKey();
            IError existing = seenKeys.put(key, error);

            assertNull(existing, String.format(
                    "DUPLICATE ERROR KEY DETECTED: '%s' is used by both [%s] and [%s]!",
                    key, existing, error
            ));
        }
    }
}
