package com.freelance.keys.tenant;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for tenant operations.
 */
public enum TenantValidationErrors implements IError {

    INVALID_TENANT_ID(
            "FL_8001",
            "The tenant identifier is required and cannot be blank.",
            "Tenant ID validation failed",
            400
    ),
    INVALID_TENANT_ID_FORMAT(
            "FL_8002",
            "Tenant ID must match pattern ^[a-zA-Z0-9_-]{2,64}$.",
            "Tenant ID format validation failed",
            400
    ),
    RESERVED_TENANT_ID(
            "FL_8003",
            "Tenant ID is a reserved system identifier.",
            "Tenant ID reserved name violation",
            400
    ),
    INVALID_TENANT_NAME(
            "FL_8004",
            "The tenant name cannot exceed 255 characters.",
            "Tenant name validation failed",
            400
    );

    private static final String CATEGORY = "tenant.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    TenantValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
        this.indirectionCode = indirectionCode;
        this.defaultMessage = defaultMessage;
        this.description = description;
        this.defaultStatusCode = defaultStatusCode;
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

    @Override
    public int getDefaultStatusCode() {
        return this.defaultStatusCode;
    }
}
