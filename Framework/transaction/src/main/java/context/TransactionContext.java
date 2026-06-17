package context;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionContext {

    private final EntityManager entityManager;

    private final Map<String, List<String>> validationErrors = new HashMap<>();
    private boolean rollbackOnly = false;

    public TransactionContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }

    // --- State Mutators ---

    public void addValidationError(String fieldName, String code) {
        this.validationErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(code);
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