package rest.response;

public class Response {
    private final int status;
    private final String body;
    private final String contentType;

    public Response(int status, String body) {
        this.status = status;
        this.body = body;
        this.contentType = "application/json; charset=UTF-8";
    }

    public int getStatus() { return status; }
    public String getBody() { return body; }
    public String getContentType() { return contentType; }
}