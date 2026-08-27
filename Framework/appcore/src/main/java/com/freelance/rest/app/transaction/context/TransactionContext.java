package com.freelance.rest.app.transaction.context;

import com.freelance.config.IError;
import com.freelance.rest.context.RequestContext;
import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds transactional and contextual data for incoming API requests.
 * <p>
 * This class is designed to be encapsulated by a wrapper, storing any state and data that must
 * be maintained throughout request processing.
 * </p>
 */
public class TransactionContext {

    private final EntityManager entityManager;

    private RequestContext requestContext = RequestContext.empty();

    private final Map<String, List<IError>> validationErrors = new ConcurrentHashMap<>();

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

    public Map<String, List<IError>> getValidationErrors() {
        return this.validationErrors;
    }

    public void addValidationError(String fieldName, IError error) {
        if (fieldName != null && error != null) {
            this.validationErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(error);
        }
    }

    /**
     * Flags this transaction to roll back rather than commit when completed.
     */
    public void markRollbackOnly() {
        this.rollbackOnly = true;
    }

    public boolean isClean() {
        return validationErrors.isEmpty() && !rollbackOnly;
    }
}
