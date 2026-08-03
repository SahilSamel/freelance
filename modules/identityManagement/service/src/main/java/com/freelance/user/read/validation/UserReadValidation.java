package com.freelance.user.read.validation;

import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.user.read.UserReadRequestDTO;

public class UserReadValidation extends AbstractValidation {
    private UserReadRequestDTO userReadRequestDTO;

    public UserReadValidation(UserReadRequestDTO userReadRequestDTO) {
        this.userReadRequestDTO = userReadRequestDTO;
    }

    public void validateRequest() {
        validate();
    }
}
