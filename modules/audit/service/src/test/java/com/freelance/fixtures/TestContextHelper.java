package com.freelance.fixtures;

import com.freelance.manager.CacheManager;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.rest.context.RequestContext;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

public class TestContextHelper {

    @SuppressWarnings("unchecked")
    public static TransactionContext setUp(UUID userId) {
        initCache();
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

    public static void initCache() {
        try {
            CacheManager mgr = CacheManager.getInstance();
            Field initialized = mgr.getClass().getDeclaredField("initialized");
            initialized.setAccessible(true);
            initialized.set(mgr, false);

            Field configsByName = mgr.getClass().getDeclaredField("configsByName");
            configsByName.setAccessible(true);
            ((Map<?, ?>) configsByName.get(mgr)).clear();

            Field nameByClass = mgr.getClass().getDeclaredField("nameByClass");
            nameByClass.setAccessible(true);
            ((Map<?, ?>) nameByClass.get(mgr)).clear();

            Field caches = mgr.getClass().getDeclaredField("caches");
            caches.setAccessible(true);
            ((Map<?, ?>) caches.get(mgr)).clear();

            mgr.init("com.freelance.fixtures");
        } catch (Exception ignored) {
        }
    }
}
