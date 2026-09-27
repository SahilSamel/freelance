package com.freelance.gym.member.constraint;

import com.freelance.gym.member.dto.MemberUpdateRequestDTO;
import com.freelance.keys.gym.member.MemberConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.read.UserReadService;

public class MemberUpdateConstraint extends AbstractConstraint {

    private final UserReadService userReadService;

    public MemberUpdateConstraint() {
        this(new UserReadService());
    }

    public MemberUpdateConstraint(UserReadService userReadService) {
        this.userReadService = userReadService;
    }

    public void checkConstraints(MemberUpdateRequestDTO request) {
        if (request == null || request.userId() == null) {
            return;
        }

        if (userReadService != null && !userReadService.existsById(request.userId())) {
            raiseConstraintError(MemberConstraintErrors.MEMBER_NOT_FOUND);
        }
    }
}

