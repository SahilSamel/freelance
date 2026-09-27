package com.freelance.keys.gym.plan;

import com.freelance.config.IError;

public enum PlanValidationErrors implements IError {

    PLAN_NAME_EMPTY(
            "FL_9001",
            "Plan name is required and cannot be blank.",
            "Plan name validation failed",
            400
    ),
    PLAN_NAME_TOO_LONG(
            "FL_9002",
            "Plan name cannot exceed 100 characters.",
            "Plan name length validation failed",
            400
    ),
    DEFAULT_PRICE_INVALID(
            "FL_9003",
            "Plan default price must be zero or a positive amount.",
            "Plan price validation failed",
            400
    ),
    PLAN_ID_REQUIRED(
            "FL_9004",
            "Plan ID is required.",
            "Plan ID validation failed",
            400
    ),
    DEFAULT_DURATION_INVALID(
            "FL_9005",
            "Plan default duration must be greater than zero.",
            "Plan duration validation failed",
            400
    );

    private static final String CATEGORY = "gym.plan.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    PlanValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
