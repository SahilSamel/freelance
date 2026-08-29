package com.freelance.authorization.service;

import com.freelance.authorization.dto.list.AuthorizationListRequestDTO;
import com.freelance.authorization.dto.list.AuthorizationListResponseDTO;
import com.freelance.authorization.service.constraint.VerifyConstraint;
import com.freelance.authorization.service.list.assembler.AuthorizationListAssembler;
import com.freelance.authorization.service.validation.VerifyValidation;
import com.freelance.entity.authorization.Authorization;
import com.freelance.repository.authorization.AuthorizationRepository;

import java.util.List;

public class AuthorizationService {

    private final AuthorizationRepository repository;
    private final AuthorizationListAssembler listAssembler;

    public AuthorizationService() {
        this(new AuthorizationRepository(), new AuthorizationListAssembler());
    }

    public AuthorizationService(AuthorizationRepository repository) {
        this(repository, new AuthorizationListAssembler());
    }

    public AuthorizationService(AuthorizationRepository repository, AuthorizationListAssembler listAssembler) {
        this.repository = repository;
        this.listAssembler = listAssembler;
    }

    public void isAuthorized() {
        new VerifyValidation().validateRequest();
        new VerifyConstraint().checkConstraints();
    }

    public AuthorizationListResponseDTO listAuthorizations(AuthorizationListRequestDTO request) {
        List<Authorization> all = repository.findAll();
        if (request != null && request.groupPath() != null && !request.groupPath().isBlank()) {
            String filter = request.groupPath().trim();
            all = all.stream()
                    .filter(a -> a.getGroupPath() != null && a.getGroupPath().equalsIgnoreCase(filter))
                    .toList();
        }
        return listAssembler.toResponseDTO(all);
    }
}
