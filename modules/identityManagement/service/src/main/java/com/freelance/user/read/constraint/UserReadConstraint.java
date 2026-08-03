package com.freelance.user.read.constraint;

import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.user.read.UserReadRequestDTO;

public class UserReadConstraint extends AbstractConstraint {

    public UserReadRequestDTO userReadRequestDTO;

    public UserReadConstraint(UserReadRequestDTO userReadRequestDTO) {
        this.userReadRequestDTO = userReadRequestDTO;
    }

    public void checkConstraints() {

    }
}
