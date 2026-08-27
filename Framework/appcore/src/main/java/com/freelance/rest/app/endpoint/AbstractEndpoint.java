package com.freelance.rest.app.endpoint;

import com.freelance.rest.app.endpoint.response.BaseResponse;
import com.freelance.rest.app.endpoint.response.handler.ResponseHandler;
import jakarta.ws.rs.core.Response;

/**
 * Serves as the abstract base class for all JAX-RS REST endpoint resources.
 * <p>
 * Provides standardized response formatting and conversion helpers to transform
 * domain transfer objects and HTTP status codes into framework {@link Response} objects.
 * </p>
 */
public abstract class AbstractEndpoint {

    private final ResponseHandler responseHandler = new ResponseHandler();

    /**
     * Builds a standardized JAX-RS {@link Response} containing the serialized DTO payload.
     *
     * @param dto        the payload object to serialize into the response body; may be {@code null}
     * @param statusCode the HTTP status code (e.g., 200, 201, 204)
     * @return a fully configured JAX-RS {@link Response} instance
     */
    protected Response buildResponse(Object dto, int statusCode) {
        BaseResponse internalRes = responseHandler.buildResponse(dto, statusCode);

        return Response.status(internalRes.getStatus())
                .entity(internalRes.getBody())
                .type(internalRes.getContentType())
                .build();
    }
}