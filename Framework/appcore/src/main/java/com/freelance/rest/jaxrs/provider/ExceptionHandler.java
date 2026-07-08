package com.freelance.rest.jaxrs.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.config.ConfigurationEngine;
import com.freelance.error.FrameworkErrorConstants;
import com.freelance.error.IBadRequestError;
import com.freelance.error.IError;
import com.freelance.rest.app.endpoint.response.BaseResponse;
import com.freelance.rest.app.service.exception.BaseException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Global JAX-RS exception mapper. Catches any {@link Throwable} thrown
 * during request processing, ensures the current transaction is rolled
 * back, and converts the failure into a standardized JSON error response.
 */
@Provider
public class ExceptionHandler implements ExceptionMapper<Throwable> { // --- 3. IMPLEMENTS MAPPER ---

    private final ObjectMapper json = new ObjectMapper();

    /**
     * JSON shape for a single field-level validation failure.
     */
    private record ValidationErrorItem(String errorCode, String fieldName, String message) {
    }

    /** JSON shape for a response containing multiple validation failures. */
    private record ValidationResponse(String errorCode, List<ValidationErrorItem> message) {
    }

    /** JSON shape for a single, non-validation error response. */
    private record StandardErrorResponse(String errorCode, String message) {
    }

    /**
     * Entry point invoked by JAX-RS whenever an unhandled exception
     * escapes an endpoint. Marks the current transaction for rollback
     * (if one exists) and delegates to {@link #buildResponse(Throwable)}.
     */
    @Override
    public Response toResponse(Throwable throwable) {

        try {
            TransactionContext ctx = Transaction.peek();
            ctx.markRollbackOnly();
        } catch (IllegalStateException e) {
            System.out.println("Shizzzzzzzzzzzzzzzzzzz");
        }

        BaseResponse internalRes = buildResponse(throwable);

        return Response.status(internalRes.getStatus())
                .entity(internalRes.getBody())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    /**
     * Maps a thrown exception to an internal {@link BaseResponse}, branching
     * on the exception type: field-level validation errors, a single
     * structured {@link BaseException}, or an unclassified failure
     * (falls back to a generic 500).
     */
    public BaseResponse buildResponse(Throwable throwable) {
        try {
            TransactionContext ctx = Transaction.peek();

            switch (throwable) {
                case ValidationException validationException -> {
                    List<ValidationErrorItem> items = getValidationErrorItems(ctx);
                    return createResponse(
                            validationException.getError().getStatusCode(),
                            new ValidationResponse(FrameworkErrorConstants.VALIDATION_ERROR.getCode(), items));
                }

                case BaseException ex -> {
                    IError error = ex.getError();
                    return createResponse(
                            error.getStatusCode(),
                            new StandardErrorResponse(error.getCode(), resolveErrorMessage(error.getConfigKey())));
                }

                case null, default -> {
                    IError internalError = FrameworkErrorConstants.INTERNAL_SERVER_ERROR;
                    return createResponse(
                            internalError.getStatusCode(),
                            new StandardErrorResponse(internalError.getCode(), resolveErrorMessage(internalError.getConfigKey())));
                }
            }

        } catch (java.lang.Exception e) {
            IError internalError = FrameworkErrorConstants.INTERNAL_SERVER_ERROR;
            return createResponse(
                    internalError.getStatusCode(),
                    new StandardErrorResponse(internalError.getCode(), resolveErrorMessage(internalError.getConfigKey())));
        }
    }

    /**
     * Converts the current transaction's accumulated field validation
     * errors into a flat list of response-ready items.
     */
    private List<ValidationErrorItem> getValidationErrorItems(TransactionContext ctx) {
        Map<String, List<IBadRequestError>> validationErrors = ctx.getValidationErrors();
        List<ValidationErrorItem> items = new ArrayList<>();

        if (validationErrors != null && !validationErrors.isEmpty()) {
            for (Map.Entry<String, List<IBadRequestError>> entry : validationErrors.entrySet()) {
                String fieldName = entry.getKey();
                List<IBadRequestError> errors = entry.getValue();

                for (IBadRequestError error : errors) {
                    items.add(new ValidationErrorItem(
                            error.getCode(),
                            fieldName,
                            resolveErrorMessage(error.getConfigKey())
                    ));
                }
            }
        }
        return items;
    }

    /**
     * Serializes the given payload to JSON and wraps it into a {@link BaseResponse}.
     */
    private BaseResponse createResponse(int statusCode, Object payload) {
        try {
            String failureJSON = json.writeValueAsString(payload);
            return new BaseResponse(statusCode, failureJSON);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response payload", e);
        }
    }

    /**
     * Resolves a human-readable error message for the given config key,
     * falling back to a generic message if no mapping is configured.
     */
    private String resolveErrorMessage(String absoluteKey) {
        if (absoluteKey == null || absoluteKey.isBlank()) {
            return "An unclassified system exception occurred.";
        }

        String mappedMessage = ConfigurationEngine.getErrorMessage(absoluteKey);

        if (mappedMessage == null || mappedMessage.isBlank()) {
            return "An unexpected exception occurred [" + absoluteKey + "]. Please try again later.";
        }

        return mappedMessage;
    }
}