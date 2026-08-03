package com.freelance.rest.app.transaction.context;

import com.freelance.error.IBadRequestError;
import com.freelance.rest.context.RequestContext;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds per-transaction state: the bound {@link EntityManager}, any
 * accumulated field validation errors, request context (logged-in user ID),
 * and whether the transaction has been flagged for rollback.
 */
public class TransactionContext {

    /**
     * The EntityManager bound to this transaction.
     */
    private final EntityManager entityManager;

    /**
     * Per-request contextual information (e.g., authenticated user ID).
     */
    private RequestContext requestContext = RequestContext.empty();

    /** Accumulated validation errors, keyed by field name. */
    private final Map<String, List<IBadRequestError>> validationErrors = new ConcurrentHashMap<>();

    /** Whether this transaction has been flagged to roll back rather than commit. */
    private boolean rollbackOnly = false;

    public TransactionContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public TransactionContext(EntityManager entityManager, RequestContext requestContext) {
        this.entityManager = entityManager;
        if (requestContext != null) {
            this.requestContext = requestContext;
        }
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext != null ? requestContext : RequestContext.empty();
    }

    public Map<String, List<IBadRequestError>> getValidationErrors() {
        return this.validationErrors;
    }

    // --- State Mutators ---

    /**
     * Records a validation error against a specific field.
     */
    public void addValidationError(String fieldName, IBadRequestError error) {
        if (fieldName != null && error != null) {
            this.validationErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(error);
        }
    }

    /**
     * Explicitly flags the transaction for rollback.
     */
    public void markRollbackOnly() {
        this.rollbackOnly = true;
    }

    // --- Evaluation ---

    /**
     * Checks whether the transaction has no validation errors and has
     * not been flagged for rollback.
     */
    public boolean isClean() {
        return validationErrors.isEmpty() && !rollbackOnly;
    }
}
