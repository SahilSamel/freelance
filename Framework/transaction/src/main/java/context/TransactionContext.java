package context;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionContext {

    private final EntityManager entityManager;
    private final Map<String, List<String>> validationErrors = new HashMap<>();
    private Throwable failure = new Throwable();

    public TransactionContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // --- Getters ---
    public EntityManager getEntityManager() {
        return entityManager;
    }
    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }
    // --- State Mutators (Used by your Bridges) ---
    public void addValidationError(String fieldName, String code) {
        this.validationErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(code);
    }

    public void addFailure(Throwable t) {
        this.failure = t;
    }

    // --- Evaluation ---
    public boolean isClean() {
        return validationErrors.isEmpty()
                && failure == null;
    }
}
