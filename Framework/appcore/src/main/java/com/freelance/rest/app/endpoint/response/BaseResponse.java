package com.freelance.rest.app.endpoint.response;

/**
 * Simple, immutable container for an HTTP response: status code, body,
 * and content type. Used as the standard return type for REST endpoints.
 */
public class BaseResponse {
    private final int status;
    private final String body;
    private final String contentType;

    /**
     * Creates a response with the default content type
     * ({@code application/json; charset=UTF-8}).
     */
    public BaseResponse(int status, String body) {
        this.status = status;
        this.body = body;
        this.contentType = "application/json; charset=UTF-8";
    }

    /**
     * Creates a response with an explicit content type.
     */
    public BaseResponse(int status, String body, String contentType) {
        this.status = status;
        this.body = body;
        this.contentType = contentType;
    }

    public int getStatus() { return status; }
    public String getBody() { return body; }
    public String getContentType() { return contentType; }
}