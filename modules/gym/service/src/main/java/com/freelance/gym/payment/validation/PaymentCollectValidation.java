package com.freelance.gym.payment.validation;

import com.freelance.gym.enums.PaymentMethod;
import com.freelance.gym.payment.dto.PaymentCollectRequestDTO;
import com.freelance.keys.gym.payment.PaymentValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

import java.math.BigDecimal;

public class PaymentCollectValidation extends AbstractValidation {

    public void validateRequest(PaymentCollectRequestDTO request) {
        if (request == null) {
            raiseValidationError(PaymentValidationErrors.PAYMENT_ID_REQUIRED, "paymentId");
            validate();
            return;
        }

        if (request.paymentId() == null) {
            raiseValidationError(PaymentValidationErrors.PAYMENT_ID_REQUIRED, "paymentId");
        }

        if (request.paidAmount() == null || request.paidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            raiseValidationError(PaymentValidationErrors.PAYMENT_AMOUNT_INVALID, "paidAmount");
        }

        if (request.paidDate() == null) {
            raiseValidationError(PaymentValidationErrors.PAID_DATE_REQUIRED, "paidDate");
        }

        if (request.paymentMethod() == null || request.paymentMethod() == PaymentMethod.NONE) {
            raiseValidationError(PaymentValidationErrors.PAYMENT_METHOD_INVALID, "paymentMethod");
        }

        if (request.transactionId() != null && request.transactionId().length() > 100) {
            raiseValidationError(PaymentValidationErrors.TRANSACTION_ID_TOO_LONG, "transactionId");
        }

        validate();
    }
}
