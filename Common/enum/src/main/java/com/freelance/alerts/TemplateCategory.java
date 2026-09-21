package com.freelance.alerts;

/**
 * Standard classification categories for messaging templates (primarily WhatsApp / Meta Business).
 * <p>
 * Non-WhatsApp channels or unspecified categories default to {@link #UTILITY}.
 * </p>
 */
public enum TemplateCategory {

    AUTHENTICATION(
            "Authentication",
            "One-time passwords, 2FA verification codes, and security alerts",
            "HIGH"
    ),
    UTILITY(
            "Utility",
            "Transactional updates, order confirmations, billing receipts, and account notifications",
            "NORMAL"
    ),
    MARKETING(
            "Marketing",
            "Promotional campaigns, offers, product announcements, and welcome messages",
            "LOW"
    );

    private final String displayName;
    private final String description;
    private final String defaultPriority;

    TemplateCategory(String displayName, String description, String defaultPriority) {
        this.displayName = displayName;
        this.description = description;
        this.defaultPriority = defaultPriority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultPriority() {
        return defaultPriority;
    }

    public static TemplateCategory fromString(String category) {
        if (category == null || category.isBlank()) {
            return UTILITY;
        }
        try {
            return TemplateCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UTILITY;
        }
    }
}
