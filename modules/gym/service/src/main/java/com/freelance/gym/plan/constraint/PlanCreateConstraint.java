package com.freelance.gym.plan.constraint;

import com.freelance.gym.plan.dto.PlanCreateRequestDTO;
import com.freelance.gym.plan.entity.PlanEntity;
import com.freelance.gym.plan.repository.PlanRepository;
import com.freelance.keys.gym.plan.PlanConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;

import java.util.Optional;

public class PlanCreateConstraint extends AbstractConstraint {

    private final PlanRepository planRepository;

    public PlanCreateConstraint() {
        this(new PlanRepository());
    }

    public PlanCreateConstraint(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public void checkConstraints(PlanCreateRequestDTO request) {
        if (request == null) {
            return;
        }

        // Check for duplicate plan name
        Optional<PlanEntity> existing = planRepository.findByName(request.name());
        if (existing.isPresent()) {
            raiseConstraintError(PlanConstraintErrors.PLAN_ALREADY_EXISTS);
        }
    }
}
