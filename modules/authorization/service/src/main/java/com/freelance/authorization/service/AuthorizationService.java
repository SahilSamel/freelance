package com.freelance.authorization.service;

import com.freelance.repository.authorization.AuthorizationRepository;
import com.freelance.authorization.service.constraint.VerifyConstraint;
import com.freelance.authorization.service.validation.VerifyValidation;

public class AuthorizationService {

    AuthorizationRepository repository;
    
    public AuthorizationService() {
        this.repository = new AuthorizationRepository();
    }

    public void isAuthorized() {
        new VerifyValidation().validateRequest();
        new VerifyConstraint().checkConstraints();
    }
}
