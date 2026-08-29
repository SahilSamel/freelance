package com.freelance.rest.app.endpoint.response.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.freelance.rest.app.endpoint.response.BaseResponse;

/**
 * Serializes transfer objects into JSON format and builds standardized {@link BaseResponse} containers.
 */
public class ResponseHandler {
    private final ObjectMapper json;

    public ResponseHandler() {
        this.json = new ObjectMapper();
        this.json.registerModule(new JavaTimeModule());
        this.json.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

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