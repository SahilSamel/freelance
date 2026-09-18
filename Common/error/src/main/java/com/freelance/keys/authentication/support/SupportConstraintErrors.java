package com.freelance.keys.authentication.support;

import com.freelance.config.IError;

/**
 * Enumerates client-facing constraint violation errors for vendor support requests.
 */
public enum SupportConstraintErrors implements IError {

    SUPPORT_ACCESS_DISABLED(
            "FL_2105",
            "Support access is currently disabled by the business owner.",
            "Vendor support attempted access while client support window was inactive or expired",
            403
    ),
    INVALID_VENDOR_SUPPORT_TOKEN(
            "FL_2106",
            "The vendor support token is invalid or expired.",
            "Cryptographic signature or timestamp verification failed for vendor support token",
            401
    );

    private static final String CATEGORY = "authentication.support.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    SupportConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
