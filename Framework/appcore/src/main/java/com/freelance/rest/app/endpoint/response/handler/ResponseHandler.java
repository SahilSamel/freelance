package com.freelance.rest.app.endpoint.response.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.rest.app.endpoint.response.BaseResponse;

public class ResponseHandler {
    private final ObjectMapper json = new ObjectMapper();
    /**
     * Maps any given DTO object into a JSON string and constructs a framework Response object.
     * @param dto Any plain-old-Java-object (POJO) or record representing your return payload.
     * @param statusCode The specific HTTP status code for this response (e.g., 200, 201, 202).
     * @return A compiled Response object ready for network transmission.
     */
    public BaseResponse buildResponse(Object dto, int statusCode) {
        try {

            String responseJSON = (dto == null) ? "{}" : json.writeValueAsString(dto);

            // Construct and return the payload package
            return new BaseResponse(statusCode, responseJSON);

        } catch (Exception e) {
            // Unchecked bubble up to prevent messy try-catch blocks inside your module endpoints
            throw new RuntimeException("ResponseHandler crashed while parsing your DTO into standard JSON format", e);
        }
    }
}