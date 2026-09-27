package com.freelance.keys.gym.member;

import com.freelance.config.IError;

public enum MemberConstraintErrors implements IError {

    MEMBER_NOT_FOUND(
            "FL_9210",
            "The specified gym member was not found.",
            "Member lookup failed",
            404
    ),
    MEMBER_ALREADY_EXISTS(
            "FL_9211",
            "A member with the given phone number or username already exists.",
            "Duplicate member conflict",
            409
    ),
    RENEWAL_DATE_OVERLAPS_EXISTING(
            "FL_9213",
            "Renewal start date cannot be before or overlap with the current active plan period.",
            "Renewal date overlap constraint violation",
            400
    );

    private static final String CATEGORY = "gym.member.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    MemberConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
