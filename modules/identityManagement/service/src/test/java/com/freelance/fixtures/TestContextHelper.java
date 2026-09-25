package com.freelance.fixtures;

import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.rest.context.RequestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.UUID;

public class TestContextHelper {

    private static final EntityManager mockEm;

    static {
        EntityTransaction mockTx = (EntityTransaction) Proxy.newProxyInstance(
                TestContextHelper.class.getClassLoader(),
                new Class<?>[]{EntityTransaction.class},
                (proxy, method, args) -> {
                    if ("isActive".equals(method.getName())) return true;
                    return null;
                }
        );

        Query mockQuery = (Query) Proxy.newProxyInstance(
                TestContextHelper.class.getClassLoader(),
                new Class<?>[]{Query.class, TypedQuery.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("setParameter".equals(name)) return proxy;
                    if ("executeUpdate".equals(name)) return 1;
                    if ("getResultList".equals(name)) return Collections.emptyList();
                    if ("getSingleResult".equals(name)) return null;
                    return null;
                }
        );

        mockEm = (EntityManager) Proxy.newProxyInstance(
                TestContextHelper.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getTransaction".equals(name)) return mockTx;
                    if ("createQuery".equals(name) || "createNamedQuery".equals(name)) return mockQuery;
                    if ("find".equals(name)) return null;
                    return null;
                }
        );
    }

    @SuppressWarnings("unchecked")
    public static TransactionContext setUp(UUID userId) {
        RequestContext rc = RequestContext.empty();
        if (userId != null) {
            rc.withUserId(userId);
        }
        TransactionContext ctx = new TransactionContext(mockEm, rc);
        try {
            Field field = Transaction.class.getDeclaredField("contextHolder");
            field.setAccessible(true);
            ((ThreadLocal<TransactionContext>) field.get(null)).set(ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ctx;
    }

    public static TransactionContext setUp() {
        return setUp(UUID.randomUUID());
    }

    @SuppressWarnings("unchecked")
    public static void tearDown() {
        try {
            Field field = Transaction.class.getDeclaredField("contextHolder");
            field.setAccessible(true);
            ((ThreadLocal<TransactionContext>) field.get(null)).remove();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
