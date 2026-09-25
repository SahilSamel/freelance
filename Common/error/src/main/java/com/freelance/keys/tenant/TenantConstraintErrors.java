package com.freelance.keys.tenant;

import com.freelance.config.IError;

/**
 * Enumerates domain constraint violation errors for tenant operations.
 */
public enum TenantConstraintErrors implements IError {

    TENANT_ALREADY_EXISTS(
            "FL_8101",
            "Tenant is already registered in the system.",
            "Tenant unique constraint violation",
            409
    ),
    CANNOT_DEACTIVATE_CONTROL_PLANE(
            "FL_8102",
            "Cannot deactivate the Control Plane tenant.",
            "Control plane protection constraint violation",
            400
    ),
    TENANT_NOT_FOUND(
            "FL_8103",
            "Tenant does not exist.",
            "Tenant lookup constraint violation",
            404
    ),
    TENANT_NOT_ACTIVE(
            "FL_8104",
            "Tenant is deactivated or not active.",
            "Tenant active status constraint violation",
            400
    );

    private static final String CATEGORY = "tenant.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    TenantConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
