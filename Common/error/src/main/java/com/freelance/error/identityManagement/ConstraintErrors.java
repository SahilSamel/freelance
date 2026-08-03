package com.freelance.error.identityManagement;

import com.freelance.error.IBadRequestError;

/**
 * Enumerates client-facing constraint violation errors for the identity
 * management domain (e.g. duplicate users, weak passwords).
 */
public enum ConstraintErrors implements IBadRequestError {

    /**
     * Raised when a user with the same identity already exists.
     */
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS") {
        @Override
        public int getStatusCode() {
            return 409;
        }
    };

    /** Config/category namespace used to build this error's config key. */
    private static final String CATEGORY = "identity.constraint";

    /** Machine-readable error code for this constant. */
    private final String code;

    ConstraintErrors(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    /**
     * Builds the fully-qualified config key for this error, e.g.
     * {@code "identity.constraint.USER_ALREADY_EXISTS"}.
     */
    @Override
    public String getConfigKey() {
        return CATEGORY + "." + this.code;
    }
}