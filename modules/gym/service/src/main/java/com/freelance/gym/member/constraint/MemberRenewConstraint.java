package com.freelance.gym.member.constraint;

import com.freelance.gym.member.dto.MemberPlanAssignmentDTO;
import com.freelance.gym.member.dto.MemberRenewRequestDTO;
import com.freelance.gym.userplan.entity.UserPlanEntity;
import com.freelance.gym.userplan.repository.UserPlanRepository;
import com.freelance.keys.gym.member.MemberConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.read.UserReadService;

import java.time.LocalDate;
import java.util.Optional;

public class MemberRenewConstraint extends AbstractConstraint {

    private final UserReadService userReadService;
    private final UserPlanRepository userPlanRepository;

    public MemberRenewConstraint() {
        this(new UserReadService(), new UserPlanRepository());
    }

    public MemberRenewConstraint(UserReadService userReadService, UserPlanRepository userPlanRepository) {
        this.userReadService = userReadService;
        this.userPlanRepository = userPlanRepository;
    }

    public void checkConstraints(MemberRenewRequestDTO request) {
        if (request == null || request.userId() == null) {
            return;
        }

        if (userReadService != null && !userReadService.existsById(request.userId())) {
            raiseConstraintError(MemberConstraintErrors.MEMBER_NOT_FOUND);
        }

        if (request.plans() != null) {
            for (MemberPlanAssignmentDTO plan : request.plans()) {
                if (plan != null && plan.planId() != null && plan.startDate() != null) {
                    Optional<UserPlanEntity> latestPlan = userPlanRepository.findLatestByUserAndPlan(request.userId(), plan.planId());
                    if (latestPlan.isPresent()) {
                        LocalDate previousEnd = latestPlan.get().getEndDate();
                        if (previousEnd != null && !plan.startDate().isAfter(previousEnd)) {
                            raiseConstraintError(MemberConstraintErrors.RENEWAL_DATE_OVERLAPS_EXISTING);
                        }
                    }
                }
            }
        }
    }
}

