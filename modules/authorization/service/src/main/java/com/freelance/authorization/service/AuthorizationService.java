package com.freelance.authorization.service;

import com.freelance.authorization.service.constraint.VerifyConstraint;
import com.freelance.authorization.service.validation.VerifyValidation;

public class AuthorizationService {
    
    public AuthorizationService() {}

    public void isAuthorized() {
        new VerifyValidation().validateRequest();
        new VerifyConstraint().checkConstraints();
    }
}
