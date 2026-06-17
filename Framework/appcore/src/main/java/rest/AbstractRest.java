package rest;

import rest.response.handler.ExceptionHandler;
import rest.response.handler.ResponseHandler;
import transaction.Transaction;

public abstract class AbstractRest {

    private final ResponseHandler responseHandler = new ResponseHandler();
    private final ExceptionHandler exceptionHandler = new ExceptionHandler();

    /**
     * SUCCESS ADAPTER: Maps to your internal .getStatus() and .getBody()
     */
    protected javax.ws.rs.core.Response buildResponse(Object dto, int statusCode) {
        rest.response.Response internalRes = responseHandler.buildResponse(dto, statusCode);

        return javax.ws.rs.core.Response.status(internalRes.getStatus())
                .entity(internalRes.getBody())
                .type(internalRes.getContentType())
                .build();
    }

    /**
     * FAILURE ADAPTER: Maps to your internal .getStatus() and .getBody()
     */
    protected javax.ws.rs.core.Response buildResponse(Throwable exception) {
        try {
            Transaction.peek().markRollbackOnly();
        } catch (IllegalStateException e) {
            // Safety fallback if the transaction never started
        }

        rest.response.Response internalRes = exceptionHandler.buildResponse(exception);

        return javax.ws.rs.core.Response.status(internalRes.getStatus())
                .entity(internalRes.getBody())
                .type(internalRes.getContentType())
                .build();
    }
}