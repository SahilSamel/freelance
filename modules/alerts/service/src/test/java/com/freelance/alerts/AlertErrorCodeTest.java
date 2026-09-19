package com.freelance.alerts;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AlertErrorCode Enum & Formatting Unit Tests")
public class AlertErrorCodeTest {

    @Test
    @DisplayName("Should cover all enum values, codes, and formatters")
    void testAllEnumConstants() {
        for (AlertErrorCode code : AlertErrorCode.values()) {
            assertNotNull(code.getCode());
            assertNotNull(code.getDescription());
            assertNotNull(code.getFormatTemplate());

            // format without args returns description
            assertEquals(code.getDescription(), code.format());
            assertEquals(code.getDescription(), code.format((Object[]) null));

            // fromCode lookup
            assertEquals(code, AlertErrorCode.fromCode(code.getCode()));
            assertEquals(code, AlertErrorCode.fromCode(code.getCode().toLowerCase()));
        }
    }

    @Test
    @DisplayName("Should format with arguments and handle formatting errors gracefully")
    void testFormatWithArgs() {
        String formatted = AlertErrorCode.CHANNEL_MISMATCH.format("EMAIL", "WHATSAPP");
        assertEquals("Template channel EMAIL does not match consumer channel WHATSAPP", formatted);

        // Invalid args format fallback
        String fallback = AlertErrorCode.CHANNEL_MISMATCH.format(new Object[]{});
        assertEquals(AlertErrorCode.CHANNEL_MISMATCH.getDescription(), fallback);

        // Invalid fromCode queries
        assertNull(AlertErrorCode.fromCode(null));
        assertNull(AlertErrorCode.fromCode(""));
        assertNull(AlertErrorCode.fromCode("   "));
        assertNull(AlertErrorCode.fromCode("NON_EXISTENT_CODE"));
    }
}
