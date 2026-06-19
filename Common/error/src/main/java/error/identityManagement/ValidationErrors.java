package error.identityManagement;

import error.IBadRequestError;

public enum ValidationErrors implements IBadRequestError {
    INVALID_EMAIL_FORMAT("INVALID_EMAIL_FORMAT"),
    PASSWORD_TOO_SHORT("PASSWORD_TOO_SHORT"),
    PASSWORD_MISSING_NUMBER("PASSWORD_MISSING_NUMBER"),
    PASSWORD_MISSING_SPECIAL_CHAR("PASSWORD_MISSING_SPECIAL_CHAR"),
    USERNAME_TOO_SHORT("USERNAME_TOO_SHORT"),
    USERNAME_HAS_INVALID_CHARS("USERNAME_HAS_INVALID_CHARS"),
    PASSWORDS_DO_NOT_MATCH("PASSWORDS_DO_NOT_MATCH");

    private static final String CATEGORY = "identity.validation";

    private final String code;

    ValidationErrors(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getConfigKey() {
        return CATEGORY + "." + this.code;
    }
}