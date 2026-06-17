package rest.response.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import context.TransactionContext;
import error.FrameworkErrorConstants;
import exception.exception.Exception;
import exception.validation.ValidationException;
import rest.response.Response;
import transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExceptionHandler {

    private final ObjectMapper json = new ObjectMapper();

    // ─── JACKSON TARGET STRUCTURES ───────────────────────────────────────────
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
                            validationException.getStatusCode(),
                            new ValidationResponse(validationException.getErrorCode(), items));
                }

                case Exception ex -> {
                    String errorCode = ex.getErrorCode();
                    return createResponse(
                            ex.getStatusCode(),
                            new StandardErrorResponse(errorCode, resolveErrorMessage(errorCode)));
                }

                case null, default -> {
                    return createResponse(
                            FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getStatusCode(),
                            new StandardErrorResponse(
                                    FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getCode(),
                                    resolveErrorMessage(FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getCode())));
                }
            }

        } catch (java.lang.Exception e) {
            return createResponse(
                    FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getStatusCode(),
                    new StandardErrorResponse(
                            FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getCode(),
                            resolveErrorMessage(FrameworkErrorConstants.INTERNAL_SERVER_ERROR.getCode())));
        }
    }

    private List<ValidationErrorItem> getValidationErrorItems(TransactionContext ctx) {
        Map<String, List<String>> validationErrors = ctx.getValidationErrors();
        List<ValidationErrorItem> items = new ArrayList<>();

        if (validationErrors != null && !validationErrors.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : validationErrors.entrySet()) {
                String fieldName = entry.getKey();
                List<String> errorCodes = entry.getValue();

                for (String errorCode : errorCodes) {
                    items.add(new ValidationErrorItem(errorCode, fieldName, resolveErrorMessage(errorCode)));
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

    private String resolveErrorMessage(String errorCode) {
        return "Just an Error Message";
    }
}