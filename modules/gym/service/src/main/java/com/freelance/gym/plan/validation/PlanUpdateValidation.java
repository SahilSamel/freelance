package com.freelance.gym.plan.validation;

import com.freelance.gym.plan.dto.PlanUpdateRequestDTO;
import com.freelance.keys.gym.plan.PlanValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

import java.math.BigDecimal;

public class PlanUpdateValidation extends AbstractValidation {

    public void validateRequest(PlanUpdateRequestDTO request) {
        if (request == null) {
            raiseValidationError(PlanValidationErrors.PLAN_ID_REQUIRED, "planId");
            validate();
            return;
        }

        if (request.planId() == null) {
            raiseValidationError(PlanValidationErrors.PLAN_ID_REQUIRED, "planId");
        }

        if (request.name() == null || request.name().isBlank()) {
            raiseValidationError(PlanValidationErrors.PLAN_NAME_EMPTY, "name");
        } else if (request.name().length() > 100) {
            raiseValidationError(PlanValidationErrors.PLAN_NAME_TOO_LONG, "name");
        }

        if (request.defaultPrice() != null && request.defaultPrice().compareTo(BigDecimal.ZERO) < 0) {
            raiseValidationError(PlanValidationErrors.DEFAULT_PRICE_INVALID, "defaultPrice");
        }

        if (request.defaultDuration() != null && request.defaultDuration() <= 0) {
            raiseValidationError(PlanValidationErrors.DEFAULT_DURATION_INVALID, "defaultDuration");
        }

        validate();
    }
}
