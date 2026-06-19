package rest.response.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.ConfigurationEngine;
import context.TransactionContext;
import error.FrameworkErrorConstants;
import error.IBadRequestError;
import error.IError;
import exception.exception.Exception;
import exception.validation.ValidationException;
import rest.response.Response;
import transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExceptionHandler {

    private final ObjectMapper json = new ObjectMapper();

    private record ValidationErrorItem(String errorCode, String fieldName, String message) {
    }

    private record ValidationResponse(String errorCode, List<ValidationErrorItem> message) {
    }

    private record StandardErrorResponse(String errorCode, String message) {
    }

    public Response buildResponse(Throwable throwable) {
        try {
            TransactionContext ctx = Transaction.peek();

            switch (throwable) {
                case ValidationException validationException -> {
                    List<ValidationErrorItem> items = getValidationErrorItems(ctx);
                    return createResponse(
                            validationException.getError().getStatusCode(),
                            new ValidationResponse(FrameworkErrorConstants.VALIDATION_ERROR.getCode(), items));
                }

                case Exception ex -> {
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

    private Response createResponse(int statusCode, Object payload) {
        try {
            String failureJSON = json.writeValueAsString(payload);
            return new Response(statusCode, failureJSON);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response payload", e);
        }
    }

    private String resolveErrorMessage(String absoluteKey) {
        if (absoluteKey == null || absoluteKey.isBlank()) {
            return "An unclassified system error occurred.";
        }

        String mappedMessage = ConfigurationEngine.getErrorMessage(absoluteKey);

        if (mappedMessage == null || mappedMessage.isBlank()) {
            return "An unexpected error occurred [" + absoluteKey + "]. Please try again later.";
        }

        return mappedMessage;
    }
}