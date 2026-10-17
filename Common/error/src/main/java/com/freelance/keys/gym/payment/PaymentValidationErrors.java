package com.freelance.keys.gym.payment;

import com.freelance.config.IError;

public enum PaymentValidationErrors implements IError {

    PAYMENT_AMOUNT_INVALID(
            "FL_9101",
            "Payment amount must be greater than zero.",
            "Payment amount validation failed",
            400
    ),
    PAID_AMOUNT_INVALID(
            "FL_9102",
            "Paid amount must be non-negative and cannot exceed the installment amount.",
            "Paid amount validation failed",
            400
    ),
    DUE_DATE_REQUIRED(
            "FL_9103",
            "Payment installment due date is required.",
            "Payment due date validation failed",
            400
    ),
    TRANSACTION_ID_TOO_LONG(
            "FL_9104",
            "Transaction ID cannot exceed 100 characters.",
            "Transaction ID validation failed",
            400
    ),
    PAYMENT_METHOD_INVALID(
            "FL_9105",
            "Payment method is invalid.",
            "Payment method validation failed",
            400
    ),
    PAYMENT_LIST_EMPTY(
            "FL_9106",
            "Payment installment schedule cannot be empty when payments are specified.",
            "Empty payment list validation failed",
            400
    ),
    PAYMENT_ID_REQUIRED(
            "FL_9107",
            "Payment ID is required.",
            "Payment ID validation failed",
            400
    ),
    PAID_DATE_REQUIRED(
            "FL_9108",
            "Paid date is required.",
            "Paid date validation failed",
            400
    );

    private static final String CATEGORY = "gym.payment.validation";

    private final String indirectionCode;
    private final String defaultMessage;
    private final String description;
    private final int defaultStatusCode;

    PaymentValidationErrors(String indirectionCode, String defaultMessage, String description, int defaultStatusCode) {
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
