package com.freelance.error.authentication;

import com.freelance.error.IBadRequestError;

public enum ConstraintErrors implements IBadRequestError {
    /**
     * Given Username doesn't exist in Identity Store
     */
    INVALID_CREDENTIALS("INVALID_CREDENTIALS"),

    /**
     * An Unauthorized request was made
     */
    UNAUTHORIZED("UNAUTHORIZED"),

    /**
     * The session claim is incorrect
     */
    INVALID_SESSION("INVALID_SESSION"),

    /**
     * The session claim is expired
     */
    EXPIRED_SESSION("EXPIRED_SESSION");

    /**
     * Config/category namespace used to build this error's config key.
     */
    private static final String CATEGORY = "authentication.constraint";

    /**
     * Machine-readable error code for this constant.
     */
    private final String code;

    ConstraintErrors(String code) {
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
