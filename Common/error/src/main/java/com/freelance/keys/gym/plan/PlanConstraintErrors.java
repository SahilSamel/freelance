package com.freelance.keys.gym.plan;

import com.freelance.config.IError;

public enum PlanConstraintErrors implements IError {

    PLAN_ALREADY_EXISTS(
            "FL_9010",
            "A plan with the given name already exists.",
            "Duplicate plan name conflict",
            409
    ),
    PLAN_NOT_FOUND(
            "FL_9011",
            "The specified plan was not found.",
            "Plan lookup failed",
            404
    );

    private static final String CATEGORY = "gym.plan.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    PlanConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
