package com.freelance.gym.member.validation;

import com.freelance.gym.member.dto.MemberPlanAssignmentDTO;
import com.freelance.gym.member.dto.MemberProfileDTO;
import com.freelance.gym.member.dto.MemberUpdateRequestDTO;
import com.freelance.gym.payment.dto.PaymentScheduleDTO;
import com.freelance.keys.gym.member.MemberValidationErrors;
import com.freelance.keys.gym.payment.PaymentValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class MemberUpdateValidation extends AbstractValidation {

    public void validateRequest(MemberUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(MemberValidationErrors.USER_ID_REQUIRED, "userId");
            validate();
            return;
        }

        if (request.userId() == null) {
            raiseValidationError(MemberValidationErrors.USER_ID_REQUIRED, "userId");
        }

        if (request.plans() != null) {
            for (int i = 0; i < request.plans().size(); i++) {
                MemberPlanAssignmentDTO plan = request.plans().get(i);
                if (plan != null && plan.startDate() != null && plan.endDate() != null) {
                    if (plan.startDate().isAfter(plan.endDate())) {
                        raiseValidationError(MemberValidationErrors.START_DATE_AFTER_END_DATE, "plans[" + i + "].startDate");
                    }
                }
            }
        }

        if (request.payments() != null) {
            for (int i = 0; i < request.payments().size(); i++) {
                PaymentScheduleDTO p = request.payments().get(i);
                if (p != null) {
                    if (p.amount() == null || p.amount().compareTo(BigDecimal.ZERO) <= 0) {
                        raiseValidationError(PaymentValidationErrors.PAYMENT_AMOUNT_INVALID, "payments[" + i + "].amount");
                    }
                    if (p.dueDate() == null) {
                        raiseValidationError(PaymentValidationErrors.DUE_DATE_REQUIRED, "payments[" + i + "].dueDate");
                    }
                }
            }
        }

        validate();
    }
}
