package com.freelance.gym.fixtures;

import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.rest.context.RequestContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.UUID;

public class TestContextHelper {

    static {
        EntityTransaction mockTx = (EntityTransaction) Proxy.newProxyInstance(
                TestContextHelper.class.getClassLoader(),
                new Class<?>[]{EntityTransaction.class},
                (proxy, method, args) -> {
                    if ("isActive".equals(method.getName())) return true;
                    return null;
                }
        );

        EntityManager mockEm = (EntityManager) Proxy.newProxyInstance(
                TestContextHelper.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    if ("getTransaction".equals(method.getName())) return mockTx;
                    return null;
                }
        );

        EntityManagerFactory mockEmf = (EntityManagerFactory) Proxy.newProxyInstance(
                TestContextHelper.class.getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if ("createEntityManager".equals(method.getName())) return mockEm;
                    return null;
                }
        );

        Transaction.init(mockEmf);
    }

    @SuppressWarnings("unchecked")
    public static TransactionContext setUp(UUID userId) {
        RequestContext rc = RequestContext.empty();
        if (userId != null) {
            rc.withUserId(userId);
        }
        TransactionContext ctx = new TransactionContext(null, rc);
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
