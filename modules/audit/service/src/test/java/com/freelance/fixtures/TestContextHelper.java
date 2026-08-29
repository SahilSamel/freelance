package com.freelance.fixtures;

import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.rest.context.RequestContext;

import java.lang.reflect.Field;
import java.util.UUID;

public class TestContextHelper {

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
