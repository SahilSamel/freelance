package error.identityManagement;

import error.IBadRequestError;

public enum ConstraintErrors implements IBadRequestError {
    USER_ALREADY_EXISTS("USER_ALREADY_EXISTS") {
        @Override
        public int getStatusCode() {
            return 409;
        }
    },
    EMAIL_ALREADY_REGISTERED("EMAIL_ALREADY_REGISTERED") {
        @Override
        public int getStatusCode() {
            return 409;
        }
    },
    PASSWORD_TOO_WEAK("PASSWORD_TOO_WEAK");

    private final String code;

    ConstraintErrors(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return this.code;
    }
}