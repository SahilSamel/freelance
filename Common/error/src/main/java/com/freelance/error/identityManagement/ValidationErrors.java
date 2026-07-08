package com.freelance.error.identityManagement;

import com.freelance.error.IBadRequestError;

/**
 * Enumerates client-facing field validation errors for the identity
 * management domain (e.g. malformed input, password rules, mismatches).
 */
public enum ValidationErrors implements IBadRequestError {

    /**
     * Email does not match a valid email format.
     */
    INVALID_EMAIL_FORMAT("INVALID_EMAIL_FORMAT"),

    /** Password does not meet the minimum length requirement. */
    PASSWORD_TOO_SHORT("PASSWORD_TOO_SHORT"),

    /** Password does not contain at least one numeric character. */
    PASSWORD_MISSING_NUMBER("PASSWORD_MISSING_NUMBER"),

    /** Password does not contain at least one special character. */
    PASSWORD_MISSING_SPECIAL_CHAR("PASSWORD_MISSING_SPECIAL_CHAR"),

    /** Username does not meet the minimum length requirement. */
    USERNAME_TOO_SHORT("USERNAME_TOO_SHORT"),

    /** Username contains characters outside the allowed set. */
    USERNAME_HAS_INVALID_CHARS("USERNAME_HAS_INVALID_CHARS"),

    /** Password and confirmation password do not match. */
    PASSWORDS_DO_NOT_MATCH("PASSWORDS_DO_NOT_MATCH");

    /** Config/category namespace used to build this error's config key. */
    private static final String CATEGORY = "identity.validation";

    /** Machine-readable error code for this constant. */
    private final String code;

    ValidationErrors(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    /**
     * Builds the fully-qualified config key for this error, e.g.
     * {@code "identity.validation.INVALID_EMAIL_FORMAT"}.
     */
    @Override
    public String getConfigKey() {
        return CATEGORY + "." + this.code;
    }
}