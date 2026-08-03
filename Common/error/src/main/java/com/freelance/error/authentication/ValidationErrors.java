package com.freelance.error.authentication;

import com.freelance.error.IBadRequestError;

public enum ValidationErrors implements IBadRequestError {

    /**
     * Invalid Username
     */
    INVALID_USERNAME("INVALID_USERNAME"),

    /**
     * Invalid password
     */
    INVALID_PASSWORD("INVALID_PASSWORD"),

    /**
     * Invalid token passed
     */
    INVALID_TOKEN("INVALID_TOKEN"),

    /**
     * Session Verification Request is invalid
     */
    INVALID_SESSION_KEY("INVALID_SESSION_KEY"),

    /**
     * Cannot create session
     */
    UNABLE_TO_CREATE_SESSION("UNABLE_TO_CREATE_SESSION");

    /**
     * Config/category namespace used to build this error's config key.
     */
    private static final String CATEGORY = "authentication.validation";

    /**
     * Machine-readable error code for this constant.
     */
    private final String code;

    ValidationErrors(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    /**
     * Builds the fully-qualified config key for this error
     */
    @Override
    public String getConfigKey() {
        return CATEGORY + "." + this.code;
    }
}
