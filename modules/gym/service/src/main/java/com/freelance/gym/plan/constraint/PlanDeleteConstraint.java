package com.freelance.gym.plan.constraint;

import com.freelance.gym.plan.dto.PlanDeleteRequestDTO;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.keys.gym.plan.PlanConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

public class PlanDeleteConstraint extends AbstractConstraint {

    private final PlanRepository planRepository;

    public PlanDeleteConstraint() {
        this(new PlanRepository());
    }

    public PlanDeleteConstraint(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public void checkConstraints(PlanDeleteRequestDTO request) {
        if (request == null || request.planId() == null) {
            return;
        }

        PlanEntity plan = planRepository.get(request.planId());
        if (plan == null) {
            raiseConstraintError(PlanConstraintErrors.PLAN_NOT_FOUND);
        }
    }
}
