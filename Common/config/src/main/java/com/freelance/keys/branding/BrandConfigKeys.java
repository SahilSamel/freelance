package com.freelance.keys.branding;

import com.freelance.config.IConfig;

/**
 * System and tenant configuration keys defining the public branding, gym name, and UI palette.
 */
public enum BrandConfigKeys implements IConfig {

    GYM_NAME(
            "gym.brand.name",
            "FitPulse",
            "Display name of the gym or fitness brand"
    ),

    GYM_SUBTITLE(
            "gym.brand.subtitle",
            "GYM OPERATOR CONSOLE",
            "Branch or subtitle description shown in top shell"
    ),

    GYM_LOGO_URL(
            "gym.brand.logoUrl",
            "",
            "Custom logo image URL or SVG for gym branding"
    ),

    THEME_PRIMARY_COLOR(
            "gym.theme.primaryColor",
            "#dc2626",
            "Primary brand accent color (HEX format)"
    ),

    THEME_SECONDARY_COLOR(
            "gym.theme.secondaryColor",
            "#2563eb",
            "Secondary accent and active navigation color (HEX format)"
    ),

    THEME_ACCENT_COLOR(
            "gym.theme.accentColor",
            "#f97316",
            "Tertiary and attention badge color (HEX format)"
    ),

    THEME_FONT_FAMILY(
            "gym.theme.fontFamily",
            "Inter, sans-serif",
            "Custom typography font family"
    );

    private final String key;
    private final String defaultValue;
    private final String description;
    private final String validation;

    BrandConfigKeys(String key, String defaultValue, String description) {
        this(key, defaultValue, description, null);
    }

    BrandConfigKeys(String key, String defaultValue, String description, String validation) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
        this.validation = validation;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getValidation() {
        return validation;
    }
}
