package com.freelance.enums;

/**
 * Declares the runtime operational environment of the application.
 * <ul>
 *   <li><b>PROD</b>: Standard runtime mode (default). Preserves database data and runs incremental migrations.</li>
 *   <li><b>TEST</b>: Test mode. Performs a complete clean-slate schema drop and rebuild on startup for clean Day-0 isolation.</li>
 * </ul>
 */
public enum DeploymentMode {
    PROD,
    TEST;

    /**
     * Parses a string value into a {@link DeploymentMode}.
     * Defaults to {@code PROD} if the input is null, blank, or unrecognized.
     *
     * @param mode input mode string (e.g. "TEST", "PROD", "dev")
     * @return matching DeploymentMode, or PROD as default
     */
    public static DeploymentMode fromString(String mode) {
        if (mode == null || mode.isBlank()) {
            return PROD;
        }
        try {
            return valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PROD;
        }
    }
}
