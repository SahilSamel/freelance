package context;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionContext {

    private final EntityManager entityManager;

    private final List<String> constraintErrors = new ArrayList<>();
    private final Map<String, List<String>> validationErrors = new HashMap<>();
    private final List<Throwable> failures = new ArrayList<>();

    public TransactionContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // --- Getters ---
    public EntityManager getEntityManager() {
        return entityManager;
    }

    public List<String> getConstraintErrors() {
        return constraintErrors;
    }

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }

    public List<Throwable> getFailures() {
        return failures;
    }

    // --- State Mutators (Used by your Bridges) ---
    public void addConstraintError(String code) {
        this.constraintErrors.add(code);
    }

    public void addValidationError(String fieldName, String code) {
        this.validationErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(code);
    }

    public void addFailure(Throwable t) {
        this.failures.add(t);
    }

    // --- Evaluation ---
    public boolean isClean() {
        return constraintErrors.isEmpty()
                && validationErrors.isEmpty()
                && failures.isEmpty();
    }
}
