package com.freelance.rest.jaxrs.provider;

import com.freelance.keys.FrameworkErrorConstants;
import com.freelance.rest.app.endpoint.response.BaseResponse;
import com.freelance.rest.app.service.exception.constraint.ConstraintException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionHandlerTest {

    private final ExceptionHandler handler = new ExceptionHandler();

    @BeforeEach
    void setUp() {
        EntityTransaction mockTx = (EntityTransaction) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityTransaction.class},
                (proxy, method, args) -> {
                    if ("isActive".equals(method.getName())) return true;
                    return null;
                }
        );

        EntityManager mockEm = (EntityManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    if ("getTransaction".equals(method.getName())) return mockTx;
                    return null;
                }
        );

        EntityManagerFactory mockEmf = (EntityManagerFactory) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{EntityManagerFactory.class},
                (proxy, method, args) -> {
                    if ("createEntityManager".equals(method.getName())) return mockEm;
                    return null;
                }
        );

        Transaction.init(mockEmf);
        Transaction.start();
    }

    @AfterEach
    void tearDown() {
        try {
            Transaction.finish();
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("buildResponse maps ValidationException with field-level errors")
    void testValidationExceptionWithFieldErrors() {
        TransactionContext ctx = Transaction.peek();
        ctx.addValidationError("email", FrameworkErrorConstants.VALIDATION_ERROR);

        BaseResponse response = handler.buildResponse(new ValidationException());
        assertNotNull(response);
        assertEquals(400, response.getStatus());

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("email"));
        assertTrue(body.contains(FrameworkErrorConstants.VALIDATION_ERROR.getIndirectionCode()));
    }

    @Test
    @DisplayName("buildResponse maps ValidationException without field errors")
    void testValidationExceptionWithoutFieldErrors() {
        BaseResponse response = handler.buildResponse(new ValidationException());
        assertNotNull(response);
        assertEquals(400, response.getStatus());

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("message\":[]"));
    }

    @Test
    @DisplayName("buildResponse maps BaseException / ConstraintException")
    void testBaseExceptionMapping() {
        BaseResponse response = handler.buildResponse(new ConstraintException(FrameworkErrorConstants.CONSTRAINT_ERROR));
        assertNotNull(response);
        assertEquals(400, response.getStatus());

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains(FrameworkErrorConstants.CONSTRAINT_ERROR.getIndirectionCode()));
    }

    @Test
    @DisplayName("buildResponse maps unexpected Throwable to 500 internal server error")
    void testUnexpectedThrowableMapping() {
        BaseResponse response = handler.buildResponse(new RuntimeException("Simulated unexpected crash"));
        assertNotNull(response);
        assertEquals(500, response.getStatus());

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains(FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getIndirectionCode()));
    }

    @Test
    @DisplayName("buildResponse maps null Throwable to 500 internal server error")
    void testNullThrowableMapping() {
        BaseResponse response = handler.buildResponse(null);
        assertNotNull(response);
        assertEquals(500, response.getStatus());
        assertTrue(response.getBody().contains(FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getIndirectionCode()));
    }

    @Test
    @DisplayName("buildResponse handles missing Transaction context gracefully")
    void testBuildResponseWithoutActiveTransaction() {
        Transaction.finish(); // Finish current transaction

        BaseResponse baseResponse = handler.buildResponse(new RuntimeException("Crash without tx"));
        assertNotNull(baseResponse);
        assertEquals(500, baseResponse.getStatus());
        assertTrue(baseResponse.getBody().contains(FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getIndirectionCode()));
    }
}
