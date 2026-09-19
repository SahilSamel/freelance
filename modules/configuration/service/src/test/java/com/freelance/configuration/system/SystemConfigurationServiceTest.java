package com.freelance.configuration.system;

import com.freelance.configuration.dto.system.list.SystemConfigurationListRequestDTO;
import com.freelance.configuration.dto.system.list.SystemConfigurationListResponseDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateRequestDTO;
import com.freelance.configuration.dto.system.update.SystemConfigurationUpdateResponseDTO;
import com.freelance.configuration.system.entity.SystemConfiguration;
import com.freelance.fixtures.FakeSystemConfigurationRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.configuration.system.SystemConfigConstraintErrors;
import com.freelance.keys.configuration.system.SystemConfigValidationErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SystemConfigurationService Tests")
class SystemConfigurationServiceTest {

    private TransactionContext context;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("listConfigurations returns all configs in groups and filters by prefix")
    void listConfigurations_works() {
        FakeSystemConfigurationRepository repo = new FakeSystemConfigurationRepository();
        repo.storage.put("auth.jwt.ttl", new SystemConfiguration("auth.jwt.ttl", "3600", "TTL"));
        repo.storage.put("identity.username.min", new SystemConfiguration("identity.username.min", "3", "Min length"));

        SystemConfigurationService service = new SystemConfigurationService(repo);
        SystemConfigurationListResponseDTO all = service.listConfigurations(SystemConfigurationListRequestDTO.empty());
        assertEquals(2, all.totalCount());
        assertEquals(2, all.groups().size());

        SystemConfigurationListResponseDTO filtered = service.listConfigurations(new SystemConfigurationListRequestDTO("auth"));
        assertEquals(1, filtered.totalCount());
        assertEquals(1, filtered.groups().size());
        assertEquals("auth", filtered.groups().get(0).name());
        assertEquals("jwt", filtered.groups().get(0).subGroups().get(0).name());
        assertEquals("auth.jwt.ttl", filtered.groups().get(0).subGroups().get(0).configurations().get(0).configId());
    }

    @Test
    @DisplayName("updateConfiguration successfully updates value and description while preserving key")
    void updateConfiguration_success() {
        FakeSystemConfigurationRepository repo = new FakeSystemConfigurationRepository();
        repo.storage.put("auth.jwt.ttl", new SystemConfiguration("auth.jwt.ttl", "3600", "Old TTL"));

        SystemConfigurationService service = new SystemConfigurationService(repo);
        SystemConfigurationUpdateResponseDTO response = service.updateConfiguration(
                new SystemConfigurationUpdateRequestDTO("auth.jwt.ttl", "7200", "New TTL")
        );

        assertNotNull(response);
        assertEquals("auth.jwt.ttl", response.configId());
        assertEquals("7200", response.configValue());
        assertEquals("New TTL", response.description());
        assertEquals(1, repo.mergeCalls);
    }

    @Test
    @DisplayName("updateConfiguration with non-existent configId raises CONFIGURATION_NOT_FOUND")
    void updateConfiguration_notFound_raisesError() {
        FakeSystemConfigurationRepository repo = new FakeSystemConfigurationRepository();

        SystemConfigurationService service = new SystemConfigurationService(repo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> service.updateConfiguration(new SystemConfigurationUpdateRequestDTO("unknown.key", "val", "desc")));
        assertEquals(SystemConfigConstraintErrors.CONFIGURATION_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("updateConfiguration with null or blank configId raises INVALID_CONFIG_ID")
    void updateConfiguration_nullId_raisesValidationError() {
        FakeSystemConfigurationRepository repo = new FakeSystemConfigurationRepository();

        SystemConfigurationService service = new SystemConfigurationService(repo);
        assertThrows(ValidationException.class,
                () -> service.updateConfiguration(new SystemConfigurationUpdateRequestDTO("", "val", "desc")));
        assertTrue(context.getValidationErrors().get("configId").contains(SystemConfigValidationErrors.INVALID_CONFIG_ID));
    }

    @Test
    @DisplayName("listConfigurations includes validation pattern in DTO")
    void listConfigurations_includesValidationPattern() {
        FakeSystemConfigurationRepository repo = new FakeSystemConfigurationRepository();
        repo.storage.put("auth.jwt.ttl", new SystemConfiguration("auth.jwt.ttl", "3600", "TTL", "^[1-9][0-9]*$"));
        repo.storage.put("unrestricted.key", new SystemConfiguration("unrestricted.key", "val", "No pattern", null));

        SystemConfigurationService service = new SystemConfigurationService(repo);
        SystemConfigurationListResponseDTO res = service.listConfigurations(SystemConfigurationListRequestDTO.empty());

        assertEquals(2, res.totalCount());
        var authConfig = res.groups().stream()
                .filter(g -> g.name().equals("auth"))
                .findFirst().orElseThrow()
                .subGroups().get(0).configurations().get(0);
        assertEquals("^[1-9][0-9]*$", authConfig.validation());

        var unconstrainedConfig = res.groups().stream()
                .filter(g -> g.name().equals("unrestricted"))
                .findFirst().orElseThrow()
                .configurations().get(0);
        assertNull(unconstrainedConfig.validation());
    }

    @Test
    @DisplayName("updateConfiguration with value violating key regex raises CONFIG_VALUE_VALIDATION_FAILED")
    void updateConfiguration_invalidValue_raisesValidationError() {
        FakeSystemConfigurationRepository repo = new FakeSystemConfigurationRepository();
        repo.storage.put("authentication.session.ttlSeconds",
                new SystemConfiguration("authentication.session.ttlSeconds", "86400", "TTL", "^[1-9][0-9]*$"));

        SystemConfigurationService service = new SystemConfigurationService(repo);
        assertThrows(ValidationException.class,
                () -> service.updateConfiguration(new SystemConfigurationUpdateRequestDTO("authentication.session.ttlSeconds", "invalid_negative_-5", "desc")));

        assertTrue(context.getValidationErrors().containsKey("configValue"));
        assertTrue(context.getValidationErrors().get("configValue").contains(SystemConfigValidationErrors.CONFIG_VALUE_VALIDATION_FAILED));
    }

    @Test
    @DisplayName("updateConfiguration with value matching key regex succeeds")
    void updateConfiguration_validValueMatchingRegex_succeeds() {
        FakeSystemConfigurationRepository repo = new FakeSystemConfigurationRepository();
        repo.storage.put("authentication.session.ttlSeconds",
                new SystemConfiguration("authentication.session.ttlSeconds", "86400", "TTL", "^[1-9][0-9]*$"));

        SystemConfigurationService service = new SystemConfigurationService(repo);
        SystemConfigurationUpdateResponseDTO res = service.updateConfiguration(
                new SystemConfigurationUpdateRequestDTO("authentication.session.ttlSeconds", "1200", "Updated TTL"));

        assertNotNull(res);
        assertEquals("1200", res.configValue());
        assertEquals("Updated TTL", res.description());
    }

    @Test
    @DisplayName("Should test all constructors, null check, and loadConfigValue in SystemConfigurationService")
    void testConstructorsAndLoadConfig() {
        assertNotNull(new SystemConfigurationService());
        FakeSystemConfigurationRepository repo = new FakeSystemConfigurationRepository();
        repo.storage.put("sys.key", new SystemConfiguration("sys.key", "val", "desc"));

        SystemConfigurationService service = new SystemConfigurationService(repo, new com.freelance.configuration.system.assembler.SystemConfigurationAssembler());
        assertNotNull(service.loadConfigValue("sys.key"));
        assertNull(service.loadConfigValue("unknown.key"));

        // SystemConfigurationConstraint tests
        com.freelance.configuration.system.constraint.SystemConfigurationConstraint c0 = new com.freelance.configuration.system.constraint.SystemConfigurationConstraint();
        assertNotNull(c0);
        assertNull(c0.checkConstraints(null));
        assertNull(c0.checkConstraints(new SystemConfigurationUpdateRequestDTO(null, "val", "desc")));
    }

    @Test
    @DisplayName("Direct test of SystemConfigurationValidation")
    void testSystemConfigValidation() {
        var val = new com.freelance.configuration.system.validation.SystemConfigurationValidation();
        assertThrows(ValidationException.class, () -> val.validateUpdateRequest(null));
        assertThrows(ValidationException.class, () -> val.validateUpdateRequest(new SystemConfigurationUpdateRequestDTO("   ", "val", "desc")));
        assertThrows(ValidationException.class, () -> val.validateUpdateRequest(new SystemConfigurationUpdateRequestDTO("key", null, "desc")));
    }
}
