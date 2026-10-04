package com.freelance.gym.plan.constraint;

import com.freelance.gym.plan.dto.PlanUpdateRequestDTO;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.keys.gym.plan.PlanConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.util.Optional;

public class PlanUpdateConstraint extends AbstractConstraint {

    private final PlanRepository planRepository;

    public PlanUpdateConstraint() {
        this(new PlanRepository());
    }

    public PlanUpdateConstraint(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public void checkConstraints(PlanUpdateRequestDTO request) {
        if (request == null || request.planId() == null) {
            return;
        }

        PlanEntity existing = planRepository.get(request.planId());
        if (existing == null) {
            raiseConstraintError(PlanConstraintErrors.PLAN_NOT_FOUND);
            return;
        }

        // If name is being changed, verify it doesn't conflict with another plan
        if (request.name() != null) {
            Optional<PlanEntity> withName = planRepository.findByName(request.name().trim());
            if (withName.isPresent() && !withName.get().getId().equals(request.planId())) {
                raiseConstraintError(PlanConstraintErrors.PLAN_ALREADY_EXISTS);
            }
        }
    }
}
