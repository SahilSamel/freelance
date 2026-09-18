package com.freelance.keys.authentication.support;

import com.freelance.config.IError;

/**
 * Enumerates client-facing field validation errors for vendor support requests.
 */
public enum SupportValidationErrors implements IError {

    INVALID_SUPPORT_REQUEST(
            "FL_2006",
            "The support access request payload is required.",
            "Support request validation failed",
            400
    ),
    INVALID_SUPPORT_REASON(
            "FL_2007",
            "The support reason is required and cannot be blank.",
            "Support reason validation failed",
            400
    ),
    INVALID_SUPPORT_TICKET(
            "FL_2008",
            "The support ticket identifier is required and cannot be blank.",
            "Support ticket validation failed",
            400
    );

    private static final String CATEGORY = "authentication.support.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    SupportValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
