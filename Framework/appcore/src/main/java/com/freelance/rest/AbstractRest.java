package com.freelance.rest;

import com.freelance.rest.response.handler.ExceptionHandler;
import com.freelance.rest.response.handler.ResponseHandler;
import com.freelance.transaction.Transaction;
import jakarta.ws.rs.core.Response;

public abstract class AbstractRest {

    private final ResponseHandler responseHandler = new ResponseHandler();
    private final ExceptionHandler exceptionHandler = new ExceptionHandler();

    /**
     * SUCCESS ADAPTER: Maps to your internal .getStatus() and .getBody()
     */
    protected Response buildResponse(Object dto, int statusCode) {
        com.freelance.rest.response.Response internalRes = responseHandler.buildResponse(dto, statusCode);

        return Response.status(internalRes.getStatus())
                .entity(internalRes.getBody())
                .type(internalRes.getContentType())
                .build();
    }

    /**
     * FAILURE ADAPTER: Maps to your internal .getStatus() and .getBody()
     */
    protected Response buildResponse(Throwable exception) {
        try {
            Transaction.peek().markRollbackOnly();
        } catch (IllegalStateException e) {
            // Safety fallback if the transaction never started
        }

        com.freelance.rest.response.Response internalRes = exceptionHandler.buildResponse(exception);

        return Response.status(internalRes.getStatus())
                .entity(internalRes.getBody())
                .type(internalRes.getContentType())
                .build();
    }
}