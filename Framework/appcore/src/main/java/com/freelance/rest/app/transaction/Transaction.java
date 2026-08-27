package com.freelance.rest.app.transaction;

import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.rest.context.RequestContext;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinates database transaction boundaries and thread-local state management per request.
 * <p>
 * Binds an isolated {@link TransactionContext} to the executing worker thread via {@link ThreadLocal},
 * ensuring safe access to the JPA {@code EntityManager}, request metadata, and accumulated validation
 * errors throughout the request pipeline.
 * </p>
 */
public class Transaction implements AutoCloseable {

    private static final String CLASS_NAME = Transaction.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger(Transaction.class);

    private static final ThreadLocal<TransactionContext> contextHolder = new ThreadLocal<>();

    private static EntityManagerFactory emf;

    public static void init(EntityManagerFactory factory) {
        emf = factory;
    }

    /**
     * Starts a new database transaction and binds an isolated transaction context to the current thread.
     * <p>
     * Any thread requiring processing in services or database operations will be required to call start()
     * before their said operations.
     * </p>
     */
    public static Transaction start() {
        if (emf == null) {
            throw new IllegalStateException("Transaction engine has not been initialized with an EntityManagerFactory!");
        }

        var em = emf.createEntityManager();
        em.getTransaction().begin();
        contextHolder.set(new TransactionContext(em));
        return new Transaction();
    }

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

    public static RequestContext getRequestContext() {
        return peek().getRequestContext();
    }

    /**
     * Finalizes and closes the active transaction bound to the current thread.
     * <p>
     * The caller must have previously called {@link #start()} within the same thread's runtime
     * before calling this method.
     * </p>
     *
     * @throws IllegalStateException if called without a matching {@link #start()} on the current thread
     */
    public static void finish() {
        TransactionContext ctx = contextHolder.get();

        if (ctx == null) {
            throw new IllegalStateException(
                    "No active transaction found to finish on this thread! " +
                            "finish() was called without a matching start()."
            );
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

    @Override
    public void close() {
        finish();
    }
}
