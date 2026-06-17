package rest;

import rest.response.ResponseHandler;
import rest.response.Response;
import error.exception.ExceptionHandler;

public abstract class AbstractRest {

    // Internal engines handling serialization and exception classification
    private final ResponseHandler responseHandler = new ResponseHandler();
    private final ExceptionHandler exceptionHandler = new ExceptionHandler();

    /**
     * SUCCESS ROUTE: Converts a business DTO into a JSON-packaged Response container.
     * @param dto Any Java serialization-ready object or record.
     * @param statusCode The target HTTP success status code (e.g., 200, 201).
     * @return An immutable Response container holding the JSON string.
     */
    protected Response buildResponse(Object dto, int statusCode) {
        return responseHandler.buildResponse(dto, statusCode);
    }

    /**
     * FAILURE ROUTE: Passes a caught exception to the handler to generate a JSON error Response container.
     * @param exception Any thrown validation, constraint, or standard Java runtime error.
     * @return An immutable Response container holding the uniform error array JSON payload.
     */
    protected Response buildResponse(Throwable exception) {
        return exceptionHandler.buildErrorResponse(exception);
    }
}