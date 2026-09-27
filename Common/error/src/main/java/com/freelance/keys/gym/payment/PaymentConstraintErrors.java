package com.freelance.keys.gym.payment;

import com.freelance.config.IError;

public enum PaymentConstraintErrors implements IError {

    PAYMENT_NOT_FOUND(
            "FL_9110",
            "The specified payment installment was not found.",
            "Payment lookup failed",
            404
    ),
    PAID_PAYMENT_CANNOT_BE_MODIFIED(
            "FL_9111",
            "Completed payments (status PAID) cannot be modified or deleted.",
            "Paid payment immutability violation",
            400
    ),
    PAYMENT_TOTAL_MISMATCH(
            "FL_9112",
            "The sum of payment installments does not match the total agreed plans price.",
            "Payment total mismatch constraint violation",
            400
    ),
    USER_PLAN_PAYMENT_MAPPING_FAILED(
            "FL_9113",
            "Failed to associate payments with the assigned user plans.",
            "Payment mapping constraint failure",
            400
    );

    private static final String CATEGORY = "gym.payment.constraint";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    PaymentConstraintErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
