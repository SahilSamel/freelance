package com.freelance.error;

/**
 * Enumerates generic, framework-level error categories used as fallbacks
 * or defaults across the application (as opposed to domain-specific errors).
 */
public enum FrameworkErrorConstants implements IError {

    /**
     * Generic validation failure.
     */
    VALIDATION_ERROR("VALIDATION_ERROR", 400),

    /** Generic constraint violation (e.g. duplicate resource, conflicting state). */
    CONSTRAINT_ERROR("CONSTRAINT_ERROR", 400),

    /** Unexpected server-side failure. */
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", 500);

    /** Machine-readable error code for this constant. */
    private final String code;

    /** HTTP status code associated with this error. */
    private final int statusCode;

    FrameworkErrorConstants(String code, int statusCode) {
        this.code = code;
        this.statusCode = statusCode;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    /** Config/category namespace used to build this error's config key. */
    private static final String CATEGORY = "framework";

    /**
     * Builds the fully-qualified config key for this error, e.g.
     * {@code "framework.VALIDATION_ERROR"}.
     */
    @Override
    public String getConfigKey() {
        return CATEGORY + "." + this.code;
    }
}