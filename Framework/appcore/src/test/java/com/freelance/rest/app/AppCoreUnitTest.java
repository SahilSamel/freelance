package com.freelance.rest.app;

import com.freelance.config.IError;
import com.freelance.rest.app.endpoint.AbstractEndpoint;
import com.freelance.rest.app.endpoint.response.BaseResponse;
import com.freelance.rest.app.endpoint.response.handler.ResponseHandler;
import com.freelance.rest.app.repository.AbstractRepository;
import com.freelance.rest.app.repository.entity.AbstractAuditEntity;
import com.freelance.rest.app.service.exception.BaseException;
import com.freelance.rest.app.service.exception.config.ConfigException;
import com.freelance.rest.app.service.exception.constraint.AbstractConstraint;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.AbstractValidation;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import com.freelance.rest.context.RequestContext;
import jakarta.persistence.*;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AppCoreUnitTest {

    public static class TestEntity extends AbstractAuditEntity {
        private Long id;
        public TestEntity() {}
        public TestEntity(Long id) { this.id = id; }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public void triggerCreate() { onCreate(); }
        public void triggerUpdate() { onUpdate(); }
    }

    public static class TestRepository extends AbstractRepository<TestEntity, Long> {
        public TestRepository() { super(); }
        public Optional<TestEntity> findSingle(String query, Map<String, Object> params) {
            return executeSingleResultSearch(query, params);
        }
        public List<TestEntity> findList(String query, Map<String, Object> params) {
            return executeListResultSearch(query, params);
        }
        public <R> List<R> findTypedList(String query, Class<R> clazz, Map<String, Object> params) {
            return executeTypedListQuery(query, clazz, params);
        }
        public int executeUpdate(String query, Map<String, Object> params) {
            return executeUpdateQuery(query, params);
        }
    }

    @SuppressWarnings("rawtypes")
    public static class NonGenericRepository extends AbstractRepository {
        public NonGenericRepository() { super(); }
    }

    private static IError createDummyError(String key, String code, int status, String msg) {
        return new IError() {
            @Override public String getErrorKey() { return key; }
            @Override public String getIndirectionCode() { return code; }
            @Override public int getDefaultStatusCode() { return status; }
            @Override public String getDefaultMessage() { return msg; }
            @Override public String getDescription() { return "Test error description"; }
        };
    }

    private EntityManager mockEm;
    private EntityTransaction mockTx;
    private EntityManagerFactory mockEmf;
    private final AtomicBoolean txActive = new AtomicBoolean(false);
    private final AtomicBoolean committed = new AtomicBoolean(false);
    private final AtomicBoolean rolledBack = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicInteger persistCount = new AtomicInteger(0);
    private final AtomicInteger mergeCount = new AtomicInteger(0);
    private final AtomicInteger removeCount = new AtomicInteger(0);
    private final AtomicInteger flushCount = new AtomicInteger(0);
    private final AtomicInteger clearCount = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        txActive.set(true);
        committed.set(false);
        rolledBack.set(false);
        closed.set(false);
        persistCount.set(0);
        mergeCount.set(0);
        removeCount.set(0);
        flushCount.set(0);
        clearCount.set(0);

        mockTx = (EntityTransaction) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityTransaction.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "isActive": return txActive.get();
                        case "begin": txActive.set(true); return null;
                        case "commit": committed.set(true); txActive.set(false); return null;
                        case "rollback": rolledBack.set(true); txActive.set(false); return null;
                        default: return null;
                    }
                }
        );

        mockEm = (EntityManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getTransaction": return mockTx;
                        case "find":
                            Long id = (Long) args[1];
                            return id != null && id > 0 ? new TestEntity(id) : null;
                        case "persist":
                            persistCount.incrementAndGet();
                            return null;
                        case "merge":
                            mergeCount.incrementAndGet();
                            return args[0];
                        case "remove":
                            removeCount.incrementAndGet();
                            return null;
                        case "contains":
                            return args[0] != null && ((TestEntity) args[0]).getId() != null && ((TestEntity) args[0]).getId() == 1L;
                        case "flush":
                            flushCount.incrementAndGet();
                            return null;
                        case "clear":
                            clearCount.incrementAndGet();
                            return null;
                        case "close":
                            closed.set(true);
                            return null;
                        case "createNamedQuery":
                            String qName = (String) args[0];
                            return createMockTypedQuery(qName);
                        default: return null;
                    }
                }
        );

        mockEmf = (EntityManagerFactory) Proxy.newProxyInstance(
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

    @SuppressWarnings("unchecked")
    private TypedQuery<?> createMockTypedQuery(String queryName) {
        return (TypedQuery<?>) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{TypedQuery.class, Query.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "setParameter":
                            return proxy;
                        case "getSingleResult":
                            if ("EmptyQuery".equals(queryName)) throw new NoResultException();
                            return new TestEntity(100L);
                        case "getResultList":
                            return List.of(new TestEntity(1L), new TestEntity(2L));
                        case "executeUpdate":
                            return 42;
                        default: return null;
                    }
                }
        );
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {}
    }

    @Test
    @DisplayName("AbstractRepository: CRUD and Batch operations")
    void testAbstractRepositoryCrudAndBatch() {
        try (var tx = Transaction.start()) {
            TestRepository repo = new TestRepository();

            assertNull(repo.get(null));
            TestEntity found = repo.get(10L);
            assertNotNull(found);
            assertEquals(10L, found.getId());

            TestEntity e1 = new TestEntity(1L);
            repo.persist(e1);
            assertEquals(1, persistCount.get());

            TestEntity merged = repo.merge(e1);
            assertNotNull(merged);
            assertEquals(1, mergeCount.get());

            repo.remove(null);
            repo.remove(e1); // contained
            repo.remove(new TestEntity(2L)); // not contained

            // Batch persist
            repo.persistEntriesInBatch(null);
            repo.persistEntriesInBatch(Collections.emptyList());
            repo.persistEntriesInBatch(List.of(new TestEntity(1L), new TestEntity(2L)), 1);
            assertTrue(flushCount.get() > 0);

            // Batch merge
            assertEquals(0, repo.mergeEntriesInBatch(null).size());
            assertEquals(0, repo.mergeEntriesInBatch(Collections.emptyList()).size());
            List<TestEntity> batchMerged = repo.mergeEntriesInBatch(Arrays.asList(new TestEntity(3L), null), 1);
            assertEquals(1, batchMerged.size());

            // Batch remove
            repo.removeEntriesInBatch(null);
            repo.removeEntriesInBatch(Collections.emptyList());
            repo.removeEntriesInBatch(Arrays.asList(new TestEntity(1L), null), 1);

            // Queries
            Optional<TestEntity> single = repo.findSingle("TestQuery", Map.of("p1", "v1"));
            assertTrue(single.isPresent());
            assertEquals(100L, single.get().getId());

            Optional<TestEntity> emptySingle = repo.findSingle("EmptyQuery", null);
            assertTrue(emptySingle.isEmpty());

            List<TestEntity> list = repo.findList("ListQuery", Map.of("k", "v"));
            assertEquals(2, list.size());

            List<TestEntity> typedList = repo.findTypedList("TypedQuery", TestEntity.class, null);
            assertEquals(2, typedList.size());

            int updated = repo.executeUpdate("UpdateQuery", Map.of("val", 123));
            assertEquals(42, updated);
        }

        NonGenericRepository nonGeneric = new NonGenericRepository();
        assertNotNull(nonGeneric);
    }

    @Test
    @DisplayName("AbstractAuditEntity: onCreate and onUpdate lifecycle")
    void testAbstractAuditEntity() {
        TestEntity entity = new TestEntity();
        assertNull(entity.getCreatedDate());
        assertNull(entity.getCreatedBy());

        // Call lifecycle methods directly
        entity.triggerCreate();
        assertNotNull(entity.getCreatedDate());
        assertNotNull(entity.getCreatedBy());
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), entity.getCreatedBy());

        // With active request context user
        try (var tx = Transaction.start()) {
            UUID testUserId = UUID.randomUUID();
            Transaction.peek().setRequestContext(new RequestContext(testUserId, "sess1", "auth1"));

            TestEntity entity2 = new TestEntity();
            entity2.triggerCreate();
            assertEquals(testUserId, entity2.getCreatedBy());
            assertEquals(testUserId, entity2.getLastUpdatedBy());

            entity2.triggerUpdate();
            assertEquals(testUserId, entity2.getLastUpdatedBy());
        }

        // Getters and setters
        UUID customUser = UUID.randomUUID();
        LocalDateTime customDate = LocalDateTime.now().minusDays(1);
        entity.setCreatedBy(customUser);
        entity.setCreatedDate(customDate);
        entity.setLastUpdatedBy(customUser);
        entity.setLastUpdatedDate(customDate);

        assertEquals(customUser, entity.getCreatedBy());
        assertEquals(customDate, entity.getCreatedDate());
        assertEquals(customUser, entity.getLastUpdatedBy());
        assertEquals(customDate, entity.getLastUpdatedDate());
    }

    @Test
    @DisplayName("Transaction and TransactionContext lifecycle")
    void testTransactionLifecycle() {
        // start without init
        Transaction.init(null);
        assertThrows(IllegalStateException.class, Transaction::start);

        Transaction.init(mockEmf);

        // peek outside transaction
        assertThrows(IllegalStateException.class, Transaction::peek);

        // finish outside transaction
        assertThrows(IllegalStateException.class, Transaction::finish);

        // Normal clean transaction
        try (var tx = Transaction.start()) {
            assertNotNull(Transaction.peek());
            assertNotNull(Transaction.getRequestContext());
            assertTrue(Transaction.peek().isClean());
        }
        assertTrue(committed.get());
        assertTrue(closed.get());

        // Dirty transaction with validation errors -> rollback
        Transaction.start();
        IError dummyError = createDummyError("test.error", "ERR_1", 400, "Error message");
        Transaction.peek().addValidationError("field1", dummyError);
        Transaction.peek().addValidationError(null, null); // ignored
        assertFalse(Transaction.peek().isClean());
        assertEquals(1, Transaction.peek().getValidationErrors().size());
        Transaction.finish();
        assertTrue(rolledBack.get());

        // Rollback only flag
        Transaction.start();
        Transaction.peek().markRollbackOnly();
        assertFalse(Transaction.peek().isClean());
        Transaction.finish();

        // TransactionContext constructor with RequestContext
        RequestContext rc = new RequestContext(UUID.randomUUID(), "sess", "auth");
        TransactionContext ctx = new TransactionContext(mockEm, rc);
        assertEquals(rc, ctx.getRequestContext());
        ctx.setRequestContext(null);
        assertNotNull(ctx.getRequestContext());
    }

    @Test
    @DisplayName("RequestContext accessors and mutators")
    void testRequestContext() {
        RequestContext empty = RequestContext.empty();
        assertNull(empty.userId());
        assertNull(empty.sessionKey());
        assertNull(empty.authorizationKey());

        UUID uid = UUID.randomUUID();
        empty.withUserId(uid);
        empty.withSessionKey("session-123");
        empty.withAuthorizationKey("ROLE_ADMIN");

        assertEquals(uid, empty.userId());
        assertEquals("session-123", empty.sessionKey());
        assertEquals("ROLE_ADMIN", empty.authorizationKey());
    }

    @Test
    @DisplayName("BaseResponse and ResponseHandler")
    void testBaseResponseAndHandler() {
        BaseResponse r1 = new BaseResponse(200, "{\"key\":\"val\"}");
        assertEquals(200, r1.getStatus());
        assertEquals("{\"key\":\"val\"}", r1.getBody());
        assertEquals("application/json; charset=UTF-8", r1.getContentType());

        BaseResponse r2 = new BaseResponse(404, "Not Found", "text/plain");
        assertEquals(404, r2.getStatus());
        assertEquals("Not Found", r2.getBody());
        assertEquals("text/plain", r2.getContentType());

        ResponseHandler handler = new ResponseHandler();
        BaseResponse nullRes = handler.buildResponse(null, 204);
        assertEquals(204, nullRes.getStatus());
        assertEquals("{}", nullRes.getBody());

        BaseResponse objRes = handler.buildResponse(Map.of("message", "hello"), 200);
        assertEquals(200, objRes.getStatus());
        assertTrue(objRes.getBody().contains("\"message\":\"hello\""));

        // Serialization exception
        Object cyclic = new Object() {
            public Object getSelf() { return this; }
        };
        assertThrows(RuntimeException.class, () -> handler.buildResponse(cyclic, 500));
    }

    @Test
    @DisplayName("AbstractEndpoint response creation")
    void testAbstractEndpoint() {
        TestEndpoint endpoint = new TestEndpoint();
        Response response = endpoint.test(Map.of("key", "value"), 201);
        assertEquals(201, response.getStatus());
        assertNotNull(response.getEntity());
    }

    private static class TestEndpoint extends AbstractEndpoint {
        public Response test(Object dto, int status) {
            return buildResponse(dto, status);
        }
    }

    private static class TestConstraint extends AbstractConstraint {
        public void test(IError err) {
            raiseConstraintError(err);
        }
    }

    private static class TestValidation extends AbstractValidation {
        public void record(IError err, String field) {
            raiseValidationError(err, field);
        }
        public void doValidate() {
            validate();
        }
    }

    @Test
    @DisplayName("AbstractConstraint and AbstractValidation error raising")
    void testConstraintAndValidation() {
        IError dummyError = createDummyError("test.invalid", "FL_1001", 400, "Invalid field");

        TestConstraint constraint = new TestConstraint();

        // null error does not throw
        assertDoesNotThrow(() -> constraint.test(null));

        ConstraintException ce = assertThrows(ConstraintException.class, () -> constraint.test(dummyError));
        assertEquals("FL_1001", ce.getError().getIndirectionCode());

        try (var tx = Transaction.start()) {
            TestValidation validation = new TestValidation();

            // clean validate does not throw
            assertDoesNotThrow(validation::doValidate);

            // ignore invalid calls
            validation.record(null, "field");
            validation.record(dummyError, null);
            validation.record(dummyError, "   ");
            assertDoesNotThrow(validation::doValidate);

            // record valid error
            validation.record(dummyError, "username");
            assertThrows(ValidationException.class, validation::doValidate);
        }
    }

    @Test
    @DisplayName("BaseException and ConfigException coverage")
    void testExceptions() {
        IError dummyError = createDummyError("test.error", "FL_9999", 500, "System error");

        BaseException be1 = new BaseException(dummyError) {};
        assertEquals("FL_9999", be1.getError().getIndirectionCode());
        assertTrue(be1.getMessage().contains("test.error"));

        BaseException be2 = new BaseException(dummyError, "Custom diag") {};
        assertEquals("Custom diag", be2.getMessage());

        BaseException be3 = new BaseException(dummyError, "Custom diag with cause", new IllegalArgumentException()) {};
        assertEquals("Custom diag with cause", be3.getMessage());
        assertNotNull(be3.getCause());

        ValidationException ve = new ValidationException();
        assertNotNull(ve.getError());

        ConfigException ce1 = new ConfigException("Config missing");
        assertEquals("Config missing", ce1.getMessage());
        assertNotNull(ce1.getError());
    }
}
