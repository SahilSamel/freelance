package com.freelance.rest.app.endpoint;

import com.freelance.rest.app.endpoint.response.BaseResponse;
import com.freelance.rest.app.endpoint.response.handler.ResponseHandler;
import com.freelance.rest.jaxrs.provider.ExceptionHandler;
import jakarta.ws.rs.core.Response;

/**
 * Base class for REST endpoint resources, providing shared helpers to
 * convert internal response/error objects into JAX-RS {@link Response} objects.
 */
public abstract class AbstractEndpoint {

    private final ResponseHandler responseHandler = new ResponseHandler();
    private final ExceptionHandler exceptionHandler = new ExceptionHandler();

    /**
     * SUCCESS ADAPTER: Maps to your internal .getStatus() and .getBody()
     */
    protected Response buildResponse(Object dto, int statusCode) {
        BaseResponse internalRes = responseHandler.buildResponse(dto, statusCode);

        return Response.status(internalRes.getStatus())
                .entity(internalRes.getBody())
                .type(internalRes.getContentType())
                .build();
    }
}