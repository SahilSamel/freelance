package com.freelance.error;

public interface IBadRequestError extends IError {

    @Override
    default int getStatusCode() {
        return 400;
    }
}