package com.freelance.error.authorization;

import com.freelance.error.IBadRequestError;

public enum ValidationErrors implements IBadRequestError {
    /**
     * An Unauthorized request was made
     */
    UNAUTHORIZED("UNAUTHORIZED");

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
