package com.freelance.rest.app.transaction;

import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.rest.context.RequestContext;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lifecycle of database transactions per API request, using a
 * {@link ThreadLocal} to bind a dedicated {@link TransactionContext}
 * (connection + execution state) to the worker thread handling the request.
 * <p>
 * Expected flow: the launcher calls {@link #start()} to begin a transaction,
 * internal bridges call {@link #peek()} to access the current thread's state,
 * and the launcher calls {@link #finish()} to commit/rollback and clean up.
 */
public class Transaction {

    private static final String CLASS_NAME = Transaction.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

    /**
     * Isolated transaction state per worker thread, to prevent cross-request data leakage.
     */
    private static final ThreadLocal<TransactionContext> contextHolder = new ThreadLocal<>();

    /** Shared JPA provider, initialized once at application boot. */
    private static EntityManagerFactory emf;

    /**
     * Binds this coordinator to the application's JPA provider. Must be
     * called exactly once at startup, before the HTTP server begins
     * accepting requests.
     */
    public static void init(EntityManagerFactory factory) {
        emf = factory;
    }

    /**
     * Begins a new transaction for the current thread: opens a fresh
     * {@code EntityManager}, starts the underlying database transaction,
     * and binds a new {@link TransactionContext} to this thread.
     *
     * @throws IllegalStateException if {@link #init(EntityManagerFactory)} was never called
     */
    public static void start() {
        if (emf == null) {
            throw new IllegalStateException("Transaction engine has not been initialized with an EntityManagerFactory!");
        }

        var em = emf.createEntityManager();
        em.getTransaction().begin();
        contextHolder.set(new TransactionContext(em));
    }

    /**
     * Retrieves the {@link TransactionContext} bound to the current thread.
     * Used internally by repositories and services to access the active
     * {@code EntityManager} or record validation errors.
     *
     * @return the active TransactionContext for this thread
     * @throws IllegalStateException if no transaction is active on this thread
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
     * Helper method to conveniently access the current thread's {@link RequestContext}.
     *
     * @return active RequestContext containing per-request data (e.g. userId)
     */
    public static RequestContext getRequestContext() {
        return peek().getRequestContext();
    }

    /**
     * Helper method to conveniently set the current thread's {@link RequestContext}.
     */
    public static void setRequestContext(RequestContext requestContext) {
        peek().setRequestContext(requestContext);
    }

    /**
     * Ends the current thread's transaction: commits if the context is
     * clean (no validation errors, not flagged for rollback), otherwise
     * rolls back. Always closes the EntityManager and clears the thread's
     * bound context, even on failure.
     *
     * @throws RuntimeException if the commit or rollback operation itself fails
     */
    public static void finish() {
        TransactionContext ctx = contextHolder.get();

        // No-op if finish() is called without a matching start() on this thread.
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
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
            } catch (Exception rollbackError) {
                logger.warn("Failure while rolling back transaction in {}", CLASS_NAME, e);
            }

            throw new RuntimeException("Critical database synchronization failure during transaction finalization.", e);

        } finally {
            if (em != null) {
                em.close();
            }
            contextHolder.remove();
        }
    }
}
