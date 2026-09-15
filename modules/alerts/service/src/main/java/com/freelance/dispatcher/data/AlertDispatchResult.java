package com.freelance.dispatcher.data;

public record AlertDispatchResult(
        Status status,
        String externalMessageId,
        String rawResponse,
        String errorCode,
        String errorMessage
) {
    public enum Status {
        SUCCESS,
        FAILURE,
        DELAYED
    }

    public static AlertDispatchResult success(String externalMessageId, String rawResponse) {
        return new AlertDispatchResult(Status.SUCCESS, externalMessageId, rawResponse, null, null);
    }

    public static AlertDispatchResult failure(String errorCode, String errorMessage, String rawResponse) {
        return new AlertDispatchResult(Status.FAILURE, null, rawResponse, errorCode, errorMessage);
    }

    public static AlertDispatchResult delayed(String details) {
        return new AlertDispatchResult(Status.DELAYED, null, details, null, details);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }

    public boolean isDelayed() {
        return status == Status.DELAYED;
    }
}
