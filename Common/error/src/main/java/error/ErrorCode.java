package error;
public enum ErrorCode {
    VALIDATION_ERROR("VALIDATION_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}