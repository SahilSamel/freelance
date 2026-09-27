package com.freelance.gym.plan.validation;

import com.freelance.gym.plan.dto.PlanDeleteRequestDTO;
import com.freelance.keys.gym.plan.PlanValidationErrors;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;

public class PlanDeleteValidation extends AbstractValidation {

    public void validateRequest(PlanDeleteRequestDTO request) {
        if (request == null || request.planId() == null) {
            raiseValidationError(PlanValidationErrors.PLAN_ID_REQUIRED, "planId");
        }
        validate();
    }
}
