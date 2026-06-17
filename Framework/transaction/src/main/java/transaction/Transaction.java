package transaction;

import context.TransactionContext;
import jakarta.persistence.EntityManagerFactory;

/**
 * COORDINATOR: Manages the lifecycle of database transactions per API request.
 * * Uses Java's ThreadLocal mechanism to bind a distinct database connection and execution
 * context (TransactionContext) directly to the specific worker thread handling the current HTTP request.
 * * Execution Flow Rule:
 * 1. Launcher calls start() -> Allocates connection, begins database transaction.
 * 2. Bridges call peek() -> Interacts with current thread's state/data.
 * 3. Launcher calls finish() -> Evaluates constraints, commits/rolls back, clears memory.
 */
public class Transaction {

    // Isolated data silo per worker thread. Prevents cross-pollination of data or connections between users.
    private static final ThreadLocal<TransactionContext> contextHolder = new ThreadLocal<>();

    // Shared reference to the database engine provider, initialized once at boot.
    private static EntityManagerFactory emf;

    /**
     * FRAMEWORK BOOTSTRAPPER
     * Links your database engine (Hibernate/JPA) to this coordinator.
     * Must be invoked exactly once inside the Launcher's Main file before booting the HTTP server.
     */
    public static void init(EntityManagerFactory factory) {
        emf = factory;
    }

    /**
     * PIPELINE ENTRY POINT: Allocates the thread's isolated database universe.
     * * Executed by the Launcher right as the HTTP request exits the gateway.
     * Spins up a fresh EntityManager, flags the database transaction start,
     * and maps a brand-new TransactionContext container to this exact thread execution path.
     */
    public static void start() {
        if (emf == null) {
            throw new IllegalStateException("Transaction engine has not been initialized with an EntityManagerFactory!");
        }

        // 1. Grab a fresh database session out of the connection pool
        var em = emf.createEntityManager();

        // 2. Open the physical transaction boundary on the database
        em.getTransaction().begin();

        // 3. Bind the connection inside a new context container to this thread
        contextHolder.set(new TransactionContext(em));
    }

    /**
     * THE MODULE HIGHWAY: Safe retrieval of the current execution state.
     * * Used exclusively by internal framework bridges (BaseRepository, BaseBusinessConstraint).
     * Allows services to fetch the running EntityManager or append validation errors
     * out of thin air, without ever needing to touch transaction lifecycle code directly.
     * * @return The active TransactionContext bound to the executing thread.
     */
    public static TransactionContext peek() {
        TransactionContext ctx = contextHolder.get();
        if (ctx == null) {
            throw new IllegalStateException(
                    "No active transaction context found on this thread! " +
                            "Database access attempted outside an established Launcher transaction pipeline."
            );
        }
        return ctx;
    }

    /**
     * PIPELINE EXIT POINT: Evaluates application state and terminates database connections.
     * * @throws RuntimeException If the database commit or rollback operation crashes.
     */
    public static void finish() {
        TransactionContext ctx = contextHolder.get();

        // Safety check if finish() is called on an uninitialized pipeline flight path
        if (ctx == null) {
            return;
        }

        var em = ctx.getEntityManager();

        try {
            if (em != null && em.getTransaction().isActive()) {
                if (ctx.isClean()) {
                    em.getTransaction().commit();
                } else {
                    em.getTransaction().rollback();
                }
            }
        } catch (Exception e) {

            try {
                if (em != null && em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } catch (Exception rollbackError) {
                // Suppressed silently to ensure 'e' remains the primary root cause exception
            }

            // 🎯 FIX: Wrap in an unchecked RuntimeException so it compiles and bubbles up perfectly!
            throw new RuntimeException("Critical database synchronization failure during transaction finalization.", e);

        } finally {
            if (em != null) {
                em.close(); // Return database connection safely back to the pool
            }
            contextHolder.remove(); // Evict the context to prevent thread-pool memory leaks
        }
    }
}