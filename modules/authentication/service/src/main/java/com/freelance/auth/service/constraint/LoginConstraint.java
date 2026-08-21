package com.freelance.auth.service.constraint;

import com.freelance.auth.dto.AuthLoginRequestDTO;
import com.freelance.keys.authentication.ConstraintErrors;
import com.freelance.auth.service.utility.password.HashUtility;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.read.User;
import com.freelance.user.read.UserReadRequestDTO;
import com.freelance.user.read.UserReadResponseDTO;

public class LoginConstraint extends AbstractConstraint {

    public void checkConstraints(AuthLoginRequestDTO request) {
        User userReadService = new User();
        UserReadRequestDTO userReadRequestDTO = new UserReadRequestDTO(null, request.username());
        UserReadResponseDTO userReadResponseDTO = userReadService.findByUsername(userReadRequestDTO);

        if (userReadResponseDTO == null) {
            raiseConstraintError(ConstraintErrors.INVALID_CREDENTIALS);
        }

        if (!HashUtility.verify(request.password(), userReadResponseDTO.userDTO().password())) {
            raiseConstraintError(ConstraintErrors.INVALID_CREDENTIALS);
        }

    }
}
