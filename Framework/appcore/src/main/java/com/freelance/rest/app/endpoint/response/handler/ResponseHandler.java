package com.freelance.rest.app.endpoint.response.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.rest.app.endpoint.response.BaseResponse;

/**
 * Serializes transfer objects into JSON format and builds standardized {@link BaseResponse} containers.
 */
public class ResponseHandler {
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Serializes the provided DTO payload into JSON and wraps it in a {@link BaseResponse}.
     *
     * @param dto        payload object or record to serialize; if {@code null}, produces {@code "{}"}
     * @param statusCode HTTP status code for the response (e.g., 200, 201)
     * @return a structured {@link BaseResponse} containing the serialized payload
     * @throws RuntimeException if JSON serialization fails unexpectedly
     */
    public BaseResponse buildResponse(Object dto, int statusCode) {
        try {
            String responseJSON = (dto == null) ? "{}" : json.writeValueAsString(dto);
            return new BaseResponse(statusCode, responseJSON);
        } catch (Exception e) {
            throw new RuntimeException("ResponseHandler crashed while parsing your DTO into standard JSON format", e);
        }
    }
}