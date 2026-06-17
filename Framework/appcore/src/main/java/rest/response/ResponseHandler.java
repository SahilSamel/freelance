package rest.response;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResponseHandler {
    private final ObjectMapper json = new ObjectMapper();
    /**
     * Maps any given DTO object into a JSON string and constructs a framework Response object.
     * @param dto Any plain-old-Java-object (POJO) or record representing your return payload.
     * @param statusCode The specific HTTP status code for this response (e.g., 200, 201, 202).
     * @return A compiled Response object ready for network transmission.
     */
    public Response buildResponse(Object dto, int statusCode) {
        try {
            // Handle null gracefully by returning an empty JSON block
            String jsonString = (dto == null) ? "{}" : json.writeValueAsString(dto);

            // Construct and return the payload package
            return new Response(statusCode, jsonString);

        } catch (Exception e) {
            // Unchecked bubble up to prevent messy try-catch blocks inside your module endpoints
            throw new RuntimeException("ResponseHandler crashed while parsing your DTO into standard JSON format", e);
        }
    }
}