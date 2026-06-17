package error;

public enum FrameworkErrorConstants implements IError {
    VALIDATION_ERROR("VALIDATION_ERROR", 400),
    CONSTRAINT_ERROR("CONSTRAINT_ERROR", 400),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", 500);

    private final String code;
    private final int statusCode;

    // Enum constructor matching your definitions above
    FrameworkErrorConstants(String code, int statusCode) {
        this.code = code;
        this.statusCode = statusCode;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }
}