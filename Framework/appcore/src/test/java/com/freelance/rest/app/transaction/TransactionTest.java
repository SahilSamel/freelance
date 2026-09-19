package com.freelance.rest.app.transaction;

import com.freelance.rest.app.transaction.context.TransactionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Transaction Management Lifecycle & Boundary Tests")
class TransactionTest {

    private AtomicBoolean txActive;
    private AtomicBoolean committed;
    private AtomicBoolean rolledBack;
    private AtomicBoolean closed;
    private boolean commitThrows;
    private boolean rollbackThrows;

    @BeforeEach
    void setUp() {
        txActive = new AtomicBoolean(false);
        committed = new AtomicBoolean(false);
        rolledBack = new AtomicBoolean(false);
        closed = new AtomicBoolean(false);
        commitThrows = false;
        rollbackThrows = false;

        EntityTransaction mockTx = (EntityTransaction) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityTransaction.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("begin".equals(name)) {
                        txActive.set(true);
                        return null;
                    }
                    if ("isActive".equals(name)) {
                        return txActive.get();
                    }
                    if ("commit".equals(name)) {
                        if (commitThrows) {
                            throw new RuntimeException("Commit failed intentionally");
                        }
                        txActive.set(false);
                        committed.set(true);
                        return null;
                    }
                    if ("rollback".equals(name)) {
                        if (rollbackThrows) {
                            throw new RuntimeException("Rollback failed intentionally");
                        }
                        txActive.set(false);
                        rolledBack.set(true);
                        return null;
                    }
                    return null;
                }
        );

        EntityManager mockEm = (EntityManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getTransaction".equals(name)) {
                        return mockTx;
                    }
                    if ("close".equals(name)) {
                        closed.set(true);
                        return null;
                    }
                    return null;
                }
        );

        EntityManagerFactory mockEmf = (EntityManagerFactory) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if ("createEntityManager".equals(method.getName())) {
                        return mockEm;
                    }
                    return null;
                }
        );

        Transaction.init(mockEmf);
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("Should start transaction, commit when clean, and close on finish")
    void testSuccessfulCommitFlow() {
        Transaction tx = Transaction.start();
        assertNotNull(tx);
        assertTrue(txActive.get());

        TransactionContext ctx = Transaction.peek();
        assertNotNull(ctx);
        assertNotNull(Transaction.getRequestContext());

        Transaction.finish();
        assertFalse(txActive.get());
        assertTrue(committed.get());
        assertFalse(rolledBack.get());
        assertTrue(closed.get());

        assertThrows(IllegalStateException.class, Transaction::peek);
    }

    @Test
    @DisplayName("Should rollback when transaction context is not clean (has errors)")
    void testRollbackOnDirtyContext() {
        try (Transaction tx = Transaction.start()) {
            TransactionContext ctx = Transaction.peek();
            ctx.markRollbackOnly();
            assertFalse(ctx.isClean());
        }

        assertFalse(committed.get());
        assertTrue(rolledBack.get());
        assertTrue(closed.get());
    }

    @Test
    @DisplayName("Should handle commit exception and attempt rollback")
    void testCommitFailureTriggersRollback() {
        commitThrows = true;

        Transaction.start();
        RuntimeException ex = assertThrows(RuntimeException.class, Transaction::finish);
        assertTrue(ex.getMessage().contains("Critical database synchronization failure"));
        assertTrue(rolledBack.get());
        assertTrue(closed.get());
    }

    @Test
    @DisplayName("Should log and swallow secondary rollback exception when commit fails")
    void testCommitFailureAndRollbackFailure() {
        commitThrows = true;
        rollbackThrows = true;

        Transaction.start();
        RuntimeException ex = assertThrows(RuntimeException.class, Transaction::finish);
        assertTrue(ex.getMessage().contains("Critical database synchronization failure"));
        assertTrue(closed.get());
    }

    @Test
    @DisplayName("Calling finish or peek without start throws IllegalStateException")
    void testMissingContextThrows() {
        assertThrows(IllegalStateException.class, Transaction::finish);
        assertThrows(IllegalStateException.class, Transaction::peek);
    }

    @Test
    @DisplayName("Calling start when EMF is null throws IllegalStateException")
    void testNullEmfThrows() {
        Transaction.init(null);
        assertThrows(IllegalStateException.class, Transaction::start);
    }
}
