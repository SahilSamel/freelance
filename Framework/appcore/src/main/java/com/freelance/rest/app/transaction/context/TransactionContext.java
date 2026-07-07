package com.freelance.rest.app.transaction.context;

import com.freelance.error.IBadRequestError;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionContext {

    private final EntityManager entityManager;

    private final Map<String, List<IBadRequestError>> validationErrors = new ConcurrentHashMap<>();

    private boolean rollbackOnly = false;

    public TransactionContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public Map<String, List<IBadRequestError>> getValidationErrors() {
        return this.validationErrors;
    }

    // --- State Mutators ---

    public void addValidationError(String fieldName, IBadRequestError error) {
        if (fieldName != null && error != null) {
            this.validationErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(error);
        }
    }

    public void addFailure(Throwable throwable) {
        this.rollbackOnly = true;
    }

    public void markRollbackOnly() {
        this.rollbackOnly = true;
    }

    // --- Evaluation ---

    public boolean isClean() {
        return validationErrors.isEmpty() && !rollbackOnly;
    }
}