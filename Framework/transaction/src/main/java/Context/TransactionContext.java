package Context;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

public class TransactionContext {

    private final EntityManager entityManager;
    private final List<String> businessConstraints = new ArrayList<>();
    private final List<String> systemConstraints = new ArrayList<>();
    private final List<Throwable> failures = new ArrayList<>();

    public TransactionContext(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // --- Getters ---
    public EntityManager getEntityManager() {
        return entityManager;
    }

    public List<String> getBusinessConstraints() {
        return businessConstraints;
    }

    public List<String> getSystemConstraints() {
        return systemConstraints;
    }

    public List<Throwable> getFailures() {
        return failures;
    }

    // --- State Mutators (Used by your Bridges) ---
    public void addBusinessConstraint(String code) {
        this.businessConstraints.add(code);
    }

    public void addSystemConstraint(String code) {
        this.systemConstraints.add(code);
    }

    public void addFailure(Throwable t) {
        this.failures.add(t);
    }

    // --- Evaluation ---
    public boolean isClean() {
        return businessConstraints.isEmpty()
                && systemConstraints.isEmpty()
                && failures.isEmpty();
    }
}
