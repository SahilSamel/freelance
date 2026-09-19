package com.freelance.enums;

/**
 * Declares the runtime operational environment of the application.
 * <ul>
 *   <li><b>DEV</b>: Default mode. Preserves database data and runs incremental migrations on restarts.</li>
 *   <li><b>TEST</b>: Test mode. Performs a complete clean-slate schema drop and rebuild on startup for clean Day-0 isolation.</li>
 *   <li><b>PROD</b>: Production mode. Hard-disables schema cleaning and enforces strict incremental migrations.</li>
 * </ul>
 */
public enum DeploymentMode {
    DEV,
    TEST,
    PROD;

    /**
     * Parses a string value into a {@link DeploymentMode}.
     * Defaults to {@code DEV} if the input is null, blank, or unrecognized.
     *
     * @param mode input mode string (e.g. "TEST", "dev")
     * @return matching DeploymentMode, or DEV as default
     */
    public static DeploymentMode fromString(String mode) {
        if (mode == null || mode.isBlank()) {
            return DEV;
        }
        try {
            return valueOf(mode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEV;
        }
    }
}
