package com.freelance.gym.member.constraint;

import com.freelance.alerts.AlertChannel;
import com.freelance.alerts.preference.service.UserContactService;
import com.freelance.gym.member.dto.MemberCreateRequestDTO;
import com.freelance.keys.gym.member.MemberConstraintErrors;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.read.UserReadService;

public class MemberCreateConstraint extends AbstractConstraint {

    private final UserReadService userReadService;
    private final UserContactService userContactService;

    public MemberCreateConstraint() {
        this(new UserReadService(), new UserContactService());
    }

    public MemberCreateConstraint(UserReadService userReadService, UserContactService userContactService) {
        this.userReadService = userReadService;
        this.userContactService = userContactService;
    }

    public void checkConstraints(MemberCreateRequestDTO request) {
        if (request == null || request.phoneNumber() == null) {
            return;
        }

        String username = "mem_" + request.phoneNumber().number();
        if (userReadService != null && userReadService.existsByUsername(username)) {
            raiseConstraintError(MemberConstraintErrors.MEMBER_ALREADY_EXISTS);
        }

        if (userContactService != null && request.phoneNumber().toE164() != null) {
            if (userContactService.findByContactAndChannel(request.phoneNumber().toE164(), AlertChannel.WHATSAPP).isPresent()) {
                raiseConstraintError(MemberConstraintErrors.MEMBER_ALREADY_EXISTS);
            }
        }
    }
}

