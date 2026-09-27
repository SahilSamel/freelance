package com.freelance.gym.payment.validation;

import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.keys.gym.payment.PaymentValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

import java.math.BigDecimal;
import java.util.List;

public class PaymentScheduleValidation extends AbstractValidation {

    public void validateScheduleList(List<PaymentScheduleDTO> payments) {
        if (payments == null || payments.isEmpty()) {
            return;
        }

        for (int i = 0; i < payments.size(); i++) {
            PaymentScheduleDTO p = payments.get(i);
            if (p == null) {
                raiseValidationError(PaymentValidationErrors.PAYMENT_AMOUNT_INVALID, "payments[" + i + "]");
                continue;
            }

            if (p.amount() == null || p.amount().compareTo(BigDecimal.ZERO) <= 0) {
                raiseValidationError(PaymentValidationErrors.PAYMENT_AMOUNT_INVALID, "payments[" + i + "].amount");
            }

            if (p.paidAmount() != null) {
                if (p.paidAmount().compareTo(BigDecimal.ZERO) < 0 || (p.amount() != null && p.paidAmount().compareTo(p.amount()) > 0)) {
                    raiseValidationError(PaymentValidationErrors.PAID_AMOUNT_INVALID, "payments[" + i + "].paidAmount");
                }
            }

            if (p.dueDate() == null) {
                raiseValidationError(PaymentValidationErrors.DUE_DATE_REQUIRED, "payments[" + i + "].dueDate");
            }

            if (p.transactionId() != null && p.transactionId().length() > 100) {
                raiseValidationError(PaymentValidationErrors.TRANSACTION_ID_TOO_LONG, "payments[" + i + "].transactionId");
            }
        }

        validate();
    }
}
