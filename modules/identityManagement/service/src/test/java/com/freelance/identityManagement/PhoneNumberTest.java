package com.freelance.identityManagement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PhoneNumber Record Tests")
class PhoneNumberTest {

    @Test
    @DisplayName("PhoneNumber constructor normalizes countryCode and strips non-digits from national number")
    void testValidPhoneNumber() {
        PhoneNumber phone = new PhoneNumber("+91", " 98765-43210 ");
        assertEquals("91", phone.countryCode());
        assertEquals("9876543210", phone.number());
        assertEquals("+919876543210", phone.toE164());
    }

    @Test
    @DisplayName("PhoneNumber.of factory creates equivalent PhoneNumber")
    void testPhoneNumberFactory() {
        PhoneNumber phone = PhoneNumber.of("1", "(555) 123-4567");
        assertEquals("1", phone.countryCode());
        assertEquals("5551234567", phone.number());
        assertEquals("+15551234567", phone.toE164());
    }

    @Test
    @DisplayName("PhoneNumber constructor throws IllegalArgumentException on null inputs")
    void testNullInputsThrow() {
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(null, "123456"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("91", null));
    }

    @Test
    @DisplayName("PhoneNumber constructor throws IllegalArgumentException on empty or non-digit inputs")
    void testInvalidInputsThrow() {
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("", "123456"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("+", "123456"));
        assertThrows(IllegalArgumentException.class, () -> new PhoneNumber("91", "abc-def"));
    }
}
