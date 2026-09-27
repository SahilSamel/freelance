package com.freelance.keys.gym.member;

import com.freelance.config.IError;

public enum MemberValidationErrors implements IError {

    MEMBER_FIRST_NAME_EMPTY(
            "FL_9201",
            "Member first name is required and cannot be blank.",
            "Member first name validation failed",
            400
    ),
    MEMBER_PHONE_EMPTY(
            "FL_9202",
            "Member phone number is required.",
            "Member phone validation failed",
            400
    ),
    PLAN_DATE_RANGE_INVALID(
            "FL_9204",
            "Subscription start date must be on or before end date.",
            "Plan date range validation failed",
            400
    ),
    START_DATE_AFTER_END_DATE(
            "FL_9205",
            "Plan start date cannot be after end date.",
            "Plan date order validation failed",
            400
    ),
    USER_ID_REQUIRED(
            "FL_9206",
            "Member user ID is required.",
            "Member user ID validation failed",
            400
    ),
    PLAN_LIST_EMPTY(
            "FL_9207",
            "At least one plan must be provided for renewals.",
            "Renewal plan list validation failed",
            400
    ),
    MEMBER_LAST_NAME_EMPTY(
            "FL_9208",
            "Member last name is required and cannot be blank.",
            "Member last name validation failed",
            400
    );

    private static final String CATEGORY = "gym.member.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    MemberValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
