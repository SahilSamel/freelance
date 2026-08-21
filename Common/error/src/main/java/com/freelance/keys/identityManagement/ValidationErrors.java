package com.freelance.keys.identityManagement;

import com.freelance.config.IBadRequestError;

/**
 * Enumerates client-facing field validation errors for the identity
 * management domain (e.g. malformed input, password rules, mismatches).
 */
public enum ValidationErrors implements IBadRequestError {

    INVALID_EMAIL_FORMAT(
            "FL_3001",
            "The email address format is invalid.",
            "Email does not match a valid email format"
    ),
    PASSWORD_TOO_SHORT(
            "FL_3002",
            "The password is shorter than the required minimum length.",
            "Password does not meet the minimum length requirement"
    ),
    PASSWORD_MISSING_NUMBER(
            "FL_3003",
            "The password must contain at least one numeric character.",
            "Password does not contain at least one numeric character"
    ),
    PASSWORD_MISSING_SPECIAL_CHAR(
            "FL_3004",
            "The password must contain at least one special character.",
            "Password does not contain at least one special character"
    ),
    USERNAME_TOO_SHORT(
            "FL_3005",
            "The username does not meet the minimum length requirement.",
            "Username does not meet the minimum length requirement"
    ),
    USERNAME_HAS_INVALID_CHARS(
            "FL_3006",
            "The username contains invalid characters.",
            "Username contains characters outside the allowed set"
    ),
    PASSWORDS_DO_NOT_MATCH(
            "FL_3007",
            "Password and confirmation password do not match.",
            "Password and confirmation password do not match"
    );

    private static final String CATEGORY = "identity.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;

    ValidationErrors(String indirectionCode, String defaultMessage, String description) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
    }

    @Override
    public String getErrorKey() {
        return CATEGORY + "." + name();
    }

    @Override
    public String getIndirectionCode() {
        return this.indirectionCode;
    }

    @Override
    public String getDefaultMessage() {
        return this.defaultMessage;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}