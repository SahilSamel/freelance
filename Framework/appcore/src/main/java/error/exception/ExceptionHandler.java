package error.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import context.TransactionContext;
import error.validation.ValidationException;
import error.constraint.ConstraintException;
import error.exception.BaseException;
import rest.response.Response;
import transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExceptionHandler {
    private final ObjectMapper json = new ObjectMapper();
    private record ErrorDetail(String errorCode, String message) {}
    private record FieldValidationError(String fieldName, List<ErrorDetail> errors) {}
    public Integer statusCode;

    public Response buildResponse(Throwable throwable){
        try{
            TransactionContext ctx = Transaction.peek();

            if(throwable instanceof ValidationException){
                Map<String, List<String>> validationErrors = ctx.getValidationErrors();
                List<FieldValidationError> payload = new ArrayList<>();

                if(validationErrors!= null && !validationErrors.isEmpty()){
                    for (Map.Entry<String, List<String>> entry : validationErrors.entrySet()) {
                        String fieldName = entry.getKey();
                        List<String> errorCodes = entry.getValue();
                        List<ErrorDetail> details = new ArrayList<>();
                        // errorCode : message
                        for (String errorCode : errorCodes) {
                            details.add(new ErrorDetail(errorCode, ""));
                        }

                        // {fieldName: "", errors:[{code : message}]}
                        payload.add(new FieldValidationError(fieldName, details));
                    }
                    return createResponse(400, payload);
                }

            } else if(throwable instanceof ConstraintException ex){
                String errorCode = ex.getErrorCode();
                return createResponse(400, new ErrorDetail(errorCode, ""));

            } else if(throwable instanceof BaseException ex){
                String errorCode = ex.getErrorCode();
                int code = statusCode == null ? 500 : statusCode;
                return createResponse(code, new ErrorDetail(errorCode, ""));

            } else{
                String payload = "Internal Server error";
                return createResponse(500, new ErrorDetail("RANDOM_EXCEPTION", payload));
            }

        } catch(Exception e){
            return createResponse(500, new ErrorDetail("RANDOM_EXCEPTION", "Server Error"));
        }
        return null;
    }

    private Response createResponse(int statusCode, Object payload) {
        this.statusCode = statusCode;
        try {
            String jsonText = json.writeValueAsString(payload);
            return new Response(statusCode, jsonText);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response payload", e);
        }
    }
}
