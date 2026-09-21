package com.freelance.alerts.service;

import com.freelance.alerts.TemplateCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCategoryTest {

    @Test
    @DisplayName("Should have exactly the 3 standard messaging categories")
    void testCategoriesList() {
        TemplateCategory[] categories = TemplateCategory.values();
        assertEquals(3, categories.length);
        assertNotNull(TemplateCategory.valueOf("AUTHENTICATION"));
        assertNotNull(TemplateCategory.valueOf("UTILITY"));
        assertNotNull(TemplateCategory.valueOf("MARKETING"));
    }

    @Test
    @DisplayName("Should return proper metadata for each category")
    void testCategoryProperties() {
        assertEquals("Authentication", TemplateCategory.AUTHENTICATION.getDisplayName());
        assertEquals("HIGH", TemplateCategory.AUTHENTICATION.getDefaultPriority());
        assertFalse(TemplateCategory.AUTHENTICATION.getDescription().isBlank());

        assertEquals("Utility", TemplateCategory.UTILITY.getDisplayName());
        assertEquals("NORMAL", TemplateCategory.UTILITY.getDefaultPriority());
        assertFalse(TemplateCategory.UTILITY.getDescription().isBlank());

        assertEquals("Marketing", TemplateCategory.MARKETING.getDisplayName());
        assertEquals("LOW", TemplateCategory.MARKETING.getDefaultPriority());
        assertFalse(TemplateCategory.MARKETING.getDescription().isBlank());
    }

    @Test
    @DisplayName("fromString should safely parse case-insensitively and default to UTILITY on null/invalid")
    void testFromString() {
        assertEquals(TemplateCategory.AUTHENTICATION, TemplateCategory.fromString("AUTHENTICATION"));
        assertEquals(TemplateCategory.AUTHENTICATION, TemplateCategory.fromString("authentication"));
        assertEquals(TemplateCategory.AUTHENTICATION, TemplateCategory.fromString("  Authentication  "));

        assertEquals(TemplateCategory.UTILITY, TemplateCategory.fromString("UTILITY"));
        assertEquals(TemplateCategory.UTILITY, TemplateCategory.fromString("utility"));

        assertEquals(TemplateCategory.MARKETING, TemplateCategory.fromString("MARKETING"));
        assertEquals(TemplateCategory.MARKETING, TemplateCategory.fromString("marketing"));

        // Defaults to UTILITY on invalid or null
        assertEquals(TemplateCategory.UTILITY, TemplateCategory.fromString(null));
        assertEquals(TemplateCategory.UTILITY, TemplateCategory.fromString(""));
        assertEquals(TemplateCategory.UTILITY, TemplateCategory.fromString("   "));
        assertEquals(TemplateCategory.UTILITY, TemplateCategory.fromString("RANDOM_INVALID_CATEGORY"));
    }
}
