package com.freelance.rest.app.endpoint.response;

/**
 * Immutable value container encapsulating an HTTP response status code,
 * serialized body, and content-type header.
 */
public class BaseResponse {
    private final int status;
    private final String body;
    private final String contentType;

    /**
     * Constructs a response with the default JSON content type ({@code "application/json; charset=UTF-8"}).
     *
     * @param status the HTTP status code (e.g., 200, 400, 500)
     * @param body   the serialized JSON response payload
     */
    public BaseResponse(int status, String body) {
        this.status = status;
        this.body = body;
        this.contentType = "application/json; charset=UTF-8";
    }

    /**
     * Constructs a response with an explicitly specified content type.
     *
     * @param status      the HTTP status code
     * @param body        the response payload string
     * @param contentType MIME type of the response (e.g., {@code "application/json"})
     */
    public BaseResponse(int status, String body, String contentType) {
        this.status = status;
        this.body = body;
        this.contentType = contentType;
    }

    /**
     * Retrieves the HTTP status code.
     *
     * @return HTTP status integer
     */
    public int getStatus() { return status; }

    /**
     * Retrieves the serialized body payload.
     *
     * @return payload body string
     */
    public String getBody() { return body; }

    /**
     * Retrieves the response content type.
     *
     * @return MIME content type string
     */
    public String getContentType() { return contentType; }
}