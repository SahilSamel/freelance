package com.freelance.configuration.error;

import com.freelance.configuration.dto.error.list.ErrorConfigurationListRequestDTO;
import com.freelance.configuration.dto.error.list.ErrorConfigurationListResponseDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateRequestDTO;
import com.freelance.configuration.dto.error.update.ErrorConfigurationUpdateResponseDTO;
import com.freelance.configuration.error.entity.ErrorConfiguration;
import com.freelance.fixtures.FakeErrorConfigurationRepository;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.configuration.ConstraintErrors;
import com.freelance.keys.configuration.ValidationErrors;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorConfigurationService Tests")
class ErrorConfigurationServiceTest {

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
    @DisplayName("listErrorConfigurations returns all error configs in groups and filters by prefix/status")
    void listErrorConfigurations_works() {
        FakeErrorConfigurationRepository repo = new FakeErrorConfigurationRepository();
        repo.storage.put("auth.error.UNAUTHORIZED", new ErrorConfiguration("auth.error.UNAUTHORIZED", 401, "Unauthorized", "Desc"));
        repo.storage.put("user.error.NOT_FOUND", new ErrorConfiguration("user.error.NOT_FOUND", 404, "User not found", "Desc"));

        ErrorConfigurationService service = new ErrorConfigurationService(repo);
        ErrorConfigurationListResponseDTO all = service.listErrorConfigurations(ErrorConfigurationListRequestDTO.empty());
        assertEquals(2, all.totalCount());
        assertEquals(2, all.groups().size());

        ErrorConfigurationListResponseDTO filtered = service.listErrorConfigurations(new ErrorConfigurationListRequestDTO("auth", null));
        assertEquals(1, filtered.totalCount());
        assertEquals(1, filtered.groups().size());
        assertEquals("auth", filtered.groups().get(0).name());
        assertEquals("error", filtered.groups().get(0).subGroups().get(0).name());
        assertEquals("auth.error.UNAUTHORIZED", filtered.groups().get(0).subGroups().get(0).errorConfigurations().get(0).errorId());
    }

    @Test
    @DisplayName("updateErrorConfiguration updates status, message, description while preserving errorId")
    void updateErrorConfiguration_success() {
        FakeErrorConfigurationRepository repo = new FakeErrorConfigurationRepository();
        repo.storage.put("user.error.NOT_FOUND", new ErrorConfiguration("user.error.NOT_FOUND", 404, "Old message", "Old desc"));

        ErrorConfigurationService service = new ErrorConfigurationService(repo);
        ErrorConfigurationUpdateResponseDTO response = service.updateErrorConfiguration(
                new ErrorConfigurationUpdateRequestDTO("user.error.NOT_FOUND", 404, "Custom user not found message", "Updated desc")
        );

        assertNotNull(response);
        assertEquals("user.error.NOT_FOUND", response.errorId());
        assertEquals("Custom user not found message", response.errorMessage());
        assertEquals("Updated desc", response.description());
        assertEquals(1, repo.mergeCalls);
    }

    @Test
    @DisplayName("updateErrorConfiguration with non-existent errorId raises ERROR_CONFIGURATION_NOT_FOUND")
    void updateErrorConfiguration_notFound_raisesError() {
        FakeErrorConfigurationRepository repo = new FakeErrorConfigurationRepository();

        ErrorConfigurationService service = new ErrorConfigurationService(repo);
        ConstraintException ex = assertThrows(ConstraintException.class,
                () -> service.updateErrorConfiguration(new ErrorConfigurationUpdateRequestDTO("unknown.error", 400, "msg", "desc")));
        assertEquals(ConstraintErrors.ERROR_CONFIGURATION_NOT_FOUND, ex.getError());
    }

    @Test
    @DisplayName("updateErrorConfiguration with invalid status code raises INVALID_STATUS_CODE")
    void updateErrorConfiguration_invalidStatus_raisesError() {
        FakeErrorConfigurationRepository repo = new FakeErrorConfigurationRepository();

        ErrorConfigurationService service = new ErrorConfigurationService(repo);
        assertThrows(ValidationException.class,
                () -> service.updateErrorConfiguration(new ErrorConfigurationUpdateRequestDTO("some.error", 999, "msg", "desc")));
        assertTrue(context.getValidationErrors().get("statusCode").contains(ValidationErrors.INVALID_STATUS_CODE));
    }
}
