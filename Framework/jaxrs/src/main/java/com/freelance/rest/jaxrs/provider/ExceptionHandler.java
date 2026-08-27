package com.freelance.rest.jaxrs.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.ConfigurationEngine;
import com.freelance.keys.FrameworkErrorConstants;
import com.freelance.config.IError;
import com.freelance.rest.app.endpoint.response.BaseResponse;
import com.freelance.rest.app.service.exception.BaseException;
import com.freelance.rest.app.service.exception.validation.ValidationException;
import com.freelance.rest.app.transaction.Transaction;
import com.freelance.rest.app.transaction.context.TransactionContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Global JAX-RS exception mapper translating server throwables into standardized API error JSON payloads.
 * <p>
 * Flags the active transaction for rollback and maps application {@link BaseException}, {@link ValidationException},
 * or unexpected {@link Throwable} instances into structured error DTOs via {@link ConfigurationEngine}.
 * </p>
 */
@Provider
public class ExceptionHandler implements ExceptionMapper<Throwable> {

    private static final String CLASS_NAME = ExceptionHandler.class.getSimpleName();

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    private final ObjectMapper json = new ObjectMapper();


    /**
     * Represents an individual field-level validation failure detailing the error code, offending field name, and message.
     */
    private record ValidationErrorItem(String errorCode, String fieldName, String message) {
    }

    /**
     * Response payload used specifically for {@link ValidationException}.
     * <p>
     * Input validation can accumulate multiple failures across different fields during request evaluation.
     * The {@code message} field is therefore a list of {@link ValidationErrorItem} entries, enabling clients
     * to inspect all field-level validation errors in a single consolidated response.
     * </p>
     */
    private record ValidationResponse(String errorCode, List<ValidationErrorItem> message) {
    }

    /**
     * Standard response payload used for all non-validation errors (domain constraints, business exceptions, and unexpected failures).
     * <p>
     * Because these failures represent a single discrete error rather than multiple field-level issues,
     * the {@code message} property is a single descriptive string.
     * </p>
     */
    private record StandardErrorResponse(String errorCode, String message) {
    }


    @Override
    public Response toResponse(Throwable throwable) {

        try {
            TransactionContext ctx = Transaction.peek();
            ctx.markRollbackOnly();
        } catch (IllegalStateException e) {
            logger.debug("No active transaction found on this thread to mark for rollback: {}", e.getMessage());
        }

        BaseResponse internalRes = buildResponse(throwable);
        logger.info("Exception occurred at: ", throwable);
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
                    IError validationError = validationException.getError();
                    int statusCode = ConfigurationEngine.getStatusCode(validationError);
                    return createResponse(
                            statusCode,
                            new ValidationResponse(validationError.getIndirectionCode(), items));
                }

                case BaseException ex -> {
                    IError error = ex.getError();
                    int statusCode = ConfigurationEngine.getStatusCode(error);
                    return createResponse(
                            statusCode,
                            new StandardErrorResponse(error.getIndirectionCode(), ConfigurationEngine.getErrorMessage(error)));
                }

                case null, default -> {
                    IError internalError = FrameworkErrorConstants.INTERNAL_SERVER_ERROR;
                    int statusCode = ConfigurationEngine.getStatusCode(internalError);
                    return createResponse(
                            statusCode,
                            new StandardErrorResponse(internalError.getIndirectionCode(), ConfigurationEngine.getErrorMessage(internalError)));
                }
            }

        } catch (java.lang.Exception e) {
            IError internalError = FrameworkErrorConstants.INTERNAL_SERVER_ERROR;
            int statusCode = ConfigurationEngine.getStatusCode(internalError);
            return createResponse(
                    statusCode,
                    new StandardErrorResponse(internalError.getIndirectionCode(), ConfigurationEngine.getErrorMessage(internalError)));
        }
    }

    private List<ValidationErrorItem> getValidationErrorItems(TransactionContext ctx) {
        Map<String, List<IError>> validationErrors = ctx.getValidationErrors();
        List<ValidationErrorItem> items = new ArrayList<>();

        if (validationErrors != null && !validationErrors.isEmpty()) {
            for (Map.Entry<String, List<IError>> entry : validationErrors.entrySet()) {
                String fieldName = entry.getKey();
                List<IError> errors = entry.getValue();

                for (IError error : errors) {
                    items.add(new ValidationErrorItem(
                            error.getIndirectionCode(),
                            fieldName,
                            ConfigurationEngine.getErrorMessage(error)
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
}