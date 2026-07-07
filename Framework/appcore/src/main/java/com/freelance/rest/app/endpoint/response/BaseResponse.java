package com.freelance.rest.app.endpoint.response;

public class BaseResponse {
    private final int status;
    private final String body;
    private final String contentType;

    public BaseResponse(int status, String body) {
        this.status = status;
        this.body = body;
        this.contentType = "application/json; charset=UTF-8";
    }

    public BaseResponse(int status, String body, String contentType) {
        this.status = status;
        this.body = body;
        this.contentType = contentType;
    }

    public int getStatus() { return status; }
    public String getBody() { return body; }
    public String getContentType() { return contentType; }
}