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

// --- 1. NEW IMPORTS ---

@Provider // --- 2. REGISTERS THIS WITH JERSEY ---
public class ExceptionHandler implements ExceptionMapper<Throwable> { // --- 3. IMPLEMENTS MAPPER ---

    private final ObjectMapper json = new ObjectMapper();

    private record ValidationErrorItem(String errorCode, String fieldName, String message) {
    }

    private record ValidationResponse(String errorCode, List<ValidationErrorItem> message) {
    }

    private record StandardErrorResponse(String errorCode, String message) {
    }

    // --- 4. NEW REQUIRED METHOD ---
    @Override
    public Response toResponse(Throwable throwable) {

        // Ensure transaction is marked for rollback if it exists
        try {
            TransactionContext ctx = Transaction.peek();
            ctx.markRollbackOnly();
        } catch (IllegalStateException e) {
            System.out.println("Shizzzzzzzzzzzzzzzzzzz");
        }

        // Delegate to your existing logic below
        BaseResponse internalRes = buildResponse(throwable);

        // Convert your BaseResponse into a Jersey Response
        return Response.status(internalRes.getStatus())
                .entity(internalRes.getBody())
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

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

    private BaseResponse createResponse(int statusCode, Object payload) {
        try {
            String failureJSON = json.writeValueAsString(payload);
            return new BaseResponse(statusCode, failureJSON);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response payload", e);
        }
    }

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