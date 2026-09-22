package com.freelance.user.create;

import com.freelance.alerts.AlertChannel;
import com.freelance.fixtures.TestContextHelper;
import com.freelance.keys.identityManagement.user.UserValidationErrors;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.user.contact.UserContactRequestDTO;
import com.freelance.user.create.validation.UserCreateValidation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserCreateValidation Tests")
class UserCreateValidationTest {

    private TransactionContext context;
    private UserCreateValidation validation;

    @BeforeEach
    void setUp() {
        context = TestContextHelper.setUp();
        validation = new UserCreateValidation();
    }

    @AfterEach
    void tearDown() {
        TestContextHelper.tearDown();
    }

    @Test
    @DisplayName("valid request with contact passes validation")
    void validRequest_passes() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("valid_user", "ValidPassword123", AlertChannel.WHATSAPP, "+1234567890");
        assertDoesNotThrow(() -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("null request raises validation error")
    void nullRequest_raisesErrors() {
        assertThrows(ValidationException.class, () -> validation.validateRequest(null));
        assertFalse(context.getValidationErrors().isEmpty());
    }

    @Test
    @DisplayName("blank username raises INVALID_USERNAME")
    void blankUsername_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("", "ValidPassword123", AlertChannel.WHATSAPP, "+1234567890");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.INVALID_USERNAME));
    }

    @Test
    @DisplayName("username too short raises USERNAME_TOO_SHORT")
    void shortUsername_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("ab", "ValidPassword123", AlertChannel.WHATSAPP, "+1234567890");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.USERNAME_TOO_SHORT));
    }

    @Test
    @DisplayName("username with invalid characters raises USERNAME_HAS_INVALID_CHARS")
    void invalidCharsUsername_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("user@name#", "ValidPassword123", AlertChannel.WHATSAPP, "+1234567890");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.USERNAME_HAS_INVALID_CHARS));
    }

    @Test
    @DisplayName("blank password raises INVALID_PASSWORD")
    void blankPassword_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("valid_user", "", AlertChannel.WHATSAPP, "+1234567890");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("password").contains(UserValidationErrors.INVALID_PASSWORD));
    }

    @Test
    @DisplayName("password too short raises PASSWORD_TOO_SHORT")
    void shortPassword_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("valid_user", "123", AlertChannel.WHATSAPP, "+1234567890");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("password").contains(UserValidationErrors.PASSWORD_TOO_SHORT));
    }

    @Test
    @DisplayName("empty contacts list raises CONTACT_INFO_REQUIRED")
    void emptyContacts_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("valid_user", "ValidPassword123", List.of(), List.of());
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("contacts").contains(UserValidationErrors.CONTACT_INFO_REQUIRED));
    }

    @Test
    @DisplayName("null contacts list raises CONTACT_INFO_REQUIRED")
    void nullContacts_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO("valid_user", "ValidPassword123", List.of(), null);
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("contacts").contains(UserValidationErrors.CONTACT_INFO_REQUIRED));
    }

    @Test
    @DisplayName("contact with null channel raises INVALID_CONTACT_CHANNEL")
    void nullContactChannel_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO(
                "valid_user",
                "ValidPassword123",
                List.of(),
                List.of(new UserContactRequestDTO(null, "+1234567890"))
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("contacts.channel").contains(UserValidationErrors.INVALID_CONTACT_CHANNEL));
    }

    @Test
    @DisplayName("contact with blank contactValue raises INVALID_CONTACT_VALUE")
    void blankContactValue_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO(
                "valid_user",
                "ValidPassword123",
                List.of(),
                List.of(new UserContactRequestDTO(AlertChannel.WHATSAPP, "   "))
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("contacts.contactValue").contains(UserValidationErrors.INVALID_CONTACT_VALUE));
    }

    @Test
    @DisplayName("duplicate contact channels raise DUPLICATE_CONTACT_CHANNEL")
    void duplicateContactChannels_raiseError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO(
                "valid_user",
                "ValidPassword123",
                List.of(),
                List.of(
                        new UserContactRequestDTO(AlertChannel.WHATSAPP, "+1234567890"),
                        new UserContactRequestDTO(AlertChannel.WHATSAPP, "+0987654321")
                )
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("contacts.channel").contains(UserValidationErrors.DUPLICATE_CONTACT_CHANNEL));
    }

    @Test
    @DisplayName("null contact value raises INVALID_CONTACT_VALUE")
    void nullContactValue_raisesError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO(
                "valid_user",
                "ValidPassword123",
                List.of(),
                List.of(new UserContactRequestDTO(AlertChannel.WHATSAPP, null))
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("contacts.contactValue").contains(UserValidationErrors.INVALID_CONTACT_VALUE));
    }

    @Test
    @DisplayName("null contact element inside list raises CONTACT_INFO_REQUIRED")
    void nullContactElement_raisesError() {
        List<UserContactRequestDTO> contactsWithNull = new java.util.ArrayList<>();
        contactsWithNull.add(null);
        UserCreateRequestDTO request = new UserCreateRequestDTO(
                "valid_user",
                "ValidPassword123",
                List.of(),
                contactsWithNull
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("contacts").contains(UserValidationErrors.CONTACT_INFO_REQUIRED));
    }

    @Test
    @DisplayName("username too long raises USERNAME_TOO_LONG")
    void longUsername_raisesError() {
        String longUsername = "a".repeat(100);
        UserCreateRequestDTO request = new UserCreateRequestDTO(longUsername, "ValidPassword123", AlertChannel.WHATSAPP, "+1234567890");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.USERNAME_TOO_LONG));
    }

    @Test
    @DisplayName("null username and password raise respective errors")
    void nullUsernameAndPassword_raisesErrors() {
        UserCreateRequestDTO request = new UserCreateRequestDTO(null, null, AlertChannel.WHATSAPP, "+1234567890");
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("username").contains(UserValidationErrors.INVALID_USERNAME));
        assertTrue(context.getValidationErrors().get("password").contains(UserValidationErrors.INVALID_PASSWORD));
    }

    @Test
    @DisplayName("duplicate contact values across distinct channels raise DUPLICATE_CONTACT_VALUE")
    void duplicateContactValues_raiseError() {
        UserCreateRequestDTO request = new UserCreateRequestDTO(
                "valid_user",
                "ValidPassword123",
                List.of(),
                List.of(
                        new UserContactRequestDTO(AlertChannel.WHATSAPP, "+1234567890"),
                        new UserContactRequestDTO(AlertChannel.PUSH, "+1234567890")
                )
        );
        assertThrows(ValidationException.class, () -> validation.validateRequest(request));
        assertTrue(context.getValidationErrors().get("contacts.contactValue").contains(UserValidationErrors.DUPLICATE_CONTACT_VALUE));
    }
}
